package eu.europeana.sitemap.service;

import eu.europeana.domain.StorageObject;
import eu.europeana.features.ObjectStorageClient;
import eu.europeana.sitemap.Constants;
import eu.europeana.sitemap.SitemapType;
import eu.europeana.sitemap.StorageFileName;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jclouds.io.Payload;
import org.jclouds.io.payloads.StringPayload;
import org.jclouds.openstack.swift.v1.domain.SwiftObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;


/**
 * Checks whether we use green or blue deployment. Also allows for switching between blue and green and deleting all
 * files from the inactive deployment
 *
 * Created by jeroen on 21-9-16.
 * Refactored by Patrick Ehlert on 11-06-2018
 */
@Service
public class ActiveDeploymentService {

    private static final Logger LOG = LogManager.getLogger(ActiveDeploymentService.class);

    private static final int PROGRESS_INTERVAL = 100;

    private final ObjectStorageClient objectStorageProvider;

    /**
     * @param objectStorageClient storage where active deployment info is saved
     */
    @Autowired
    public ActiveDeploymentService(ObjectStorageClient objectStorageClient) {
        LOG.debug("Init");
        this.objectStorageProvider = objectStorageClient;
    }

    /**
     * Returns the active deployment (blue or green), depending which value is in the active file. If no active file is
     * present we create a new one with the value "green"
     * @param sitemapType type of sitemap (record or entity)     *
     */
    public Deployment getActiveDeployment(SitemapType sitemapType) {
        Deployment result = null;
        String activeFileName = StorageFileName.getActiveDeploymentFileName(sitemapType);
        if (objectStorageProvider.isAvailable(activeFileName)) { // note that isAvailable() does a head request to S3!
            LOG.debug("Reading active file {}", activeFileName);
            Optional<StorageObject> optional = objectStorageProvider.get(activeFileName);
            if (optional.isPresent()) {
                StorageObject storageObject = optional.get();
                try (InputStream in = storageObject.getPayload().openStream();
                     StringWriter writer = new StringWriter()) {
                    IOUtils.copy(in, writer);
                    result = Deployment.fromString(writer.toString());
                    storageObject.getPayload().close();
                } catch (IOException e) {
                    LOG.error("Error while processing the file {} to determine the current active site map", activeFileName, e);
                }
            } else {
                LOG.error("Active file should be present, but was not retrieved!");
            }
        } else {
            // if the active file does not exist we create a new one
            LOG.debug("Initializing active deployment file...");
            saveToStorageProvider(Deployment.GREEN, activeFileName);
            result = Deployment.GREEN;
        }
        LOG.debug("Returning deployment {}", result);
        return result;
    }

    /**
     * Returns the inactive deployment (blue or green)
     */
    public Deployment getInactiveDeployment(SitemapType sitemapType) {
        if (Deployment.BLUE == getActiveDeployment(sitemapType)) {
            return Deployment.GREEN;
        }
        return Deployment.BLUE;
    }

    /**
     * Deletes all the inactive files
     * @return the number of deleted files
     */
    public long deleteInactiveFiles(SitemapType sitemapType) {
        List<StorageObject> listFiles = objectStorageProvider.list();
        if (listFiles.isEmpty()) {
            LOG.info("No files to remove.");
            return 0;
        } else {
            Deployment inactive = this.getInactiveDeployment(sitemapType);
            String fileNameToDelete = StorageFileName.getSitemapFileName(sitemapType, inactive, null);
            // remove .xml extension so we delete also the index file
            fileNameToDelete = fileNameToDelete.split(Constants.XML_EXTENSION)[0];
   
            long i = 0;
            LOG.info("Deleting all old files with the name {} ...", fileNameToDelete);
            for (StorageObject obj : listFiles) {
                if (obj.getName().startsWith(fileNameToDelete)) {
                    LOG.debug("Deleting file {}", obj.getName());
                    objectStorageProvider.delete(obj.getName());
                    i++;
                }
                // report on progress
                if (i > 0 && i % PROGRESS_INTERVAL == 0) {
                    LOG.info("Deleted {} old files", i);
                }
            }
            LOG.info("Deleted all {} old files", i);
            return i;
        }
    }

    /**
     * Switch between blue/green deployment
     * @return the now active deployment (blue/green)
     */
    public Deployment switchDeployment(SitemapType sitemapType) {
        Deployment switchTo = getInactiveDeployment(sitemapType);
        String activeFileName = StorageFileName.getActiveDeploymentFileName(sitemapType);
        saveToStorageProvider(switchTo, activeFileName);
        return switchTo;
    }

    /**
     * Creates a new active file or updates it's contents
     *
     * @param blueGreen corresponds to {@link SwiftObject#getPayload()}
     * @return {@link SwiftObject#getETag()} of the object.
     */
    private String saveToStorageProvider(Deployment blueGreen, String activeFileName) {
        Payload payload = new StringPayload(blueGreen.toString());
        LOG.debug("Saving value {} in file {} ", blueGreen, activeFileName);
        return objectStorageProvider.put(activeFileName, payload);
    }

}
