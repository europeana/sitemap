package eu.europeana.sitemap.service;

import eu.europeana.domain.StorageObject;
import eu.europeana.features.ObjectStorageClient;
import eu.europeana.sitemap.FileNames;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jclouds.io.Payload;
import org.jclouds.io.payloads.StringPayload;
import org.jclouds.openstack.swift.v1.domain.SwiftObject;
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
public class ActiveSitemapService {

    private static final Logger LOG = LogManager.getLogger(ActiveSitemapService.class);

    private final ObjectStorageClient objectStorageProvider;
    private final String fileNameBase;
    private final String activeFileName;

    /**
     * @param objectStorageClient
     * @param fileNameBase
     */
    public ActiveSitemapService(ObjectStorageClient objectStorageClient, String fileNameBase) {
        LOG.debug("init");
        this.objectStorageProvider = objectStorageClient;
        this.fileNameBase = fileNameBase;
        this.activeFileName = FileNames.getActiveDeploymentFileName(fileNameBase);
    }

    /**
     * @return a blue or a green deployment, depending on the value in the active file. If no active file was present
     * we create a new one with the value "green"
     */
    public BlueGreenDeployment getActiveFile() {
        BlueGreenDeployment result = null;
        if (objectStorageProvider.isAvailable(activeFileName)) {
            Optional<StorageObject> optional = objectStorageProvider.get(activeFileName);
            if (optional.isPresent()) {
                StorageObject storageObject = optional.get();
                try (InputStream in = storageObject.getPayload().openStream();
                     StringWriter writer = new StringWriter()) {
                    IOUtils.copy(in, writer);
                    result = BlueGreenDeployment.valueOf(writer.toString());
                    storageObject.getPayload().close();
                } catch (IOException e) {
                    LOG.error("Error while processing the file {} to determine the current active site map", activeFileName, e);
                }
            } else {
                LOG.error("Active file should be present, but was not retrieved!");
            }
        } else {
            // if the active file does not exist we create a new one
            saveToStorageProvider(BlueGreenDeployment.GREEN);
            result = BlueGreenDeployment.GREEN;
        }
        return result;
    }

    /**
     *
     * @return the inactive sitemap file name (blue/green)
     */
    public BlueGreenDeployment getInactiveFile() {
        if (BlueGreenDeployment.BLUE.equals(getActiveFile())) {
            return BlueGreenDeployment.BLUE;
        }
        return BlueGreenDeployment.GREEN;
    }

    /**
     * Deletes all the inactive files
     */
    public void deleteInactiveFiles() {
        List<StorageObject> listFiles = objectStorageProvider.list();
        if (listFiles.isEmpty()) {
            LOG.info("No files to remove.");
        } else {
            BlueGreenDeployment inactive = this.getInactiveFile();
            String fileNameToDelete = FileNames.getSitemapFileNameStorage(fileNameBase, inactive, null);
   
            int i = 0;
            LOG.info("Deleting all old files with the name {}", fileNameToDelete);
            for (StorageObject obj : listFiles) {
                if (obj.getName().startsWith(fileNameToDelete)) {
                    objectStorageProvider.delete(obj.getName());
                    i++;
                }
                // report on progress
                if (i > 0 && i % 100 == 0) {
                    LOG.info("Removed {} old files", i);
                }
            }
            LOG.info("Removed all {} old files", i);
        }
    }

    /**
     * Switch between blue/green sitemap files
     * @return the now active sitemap file name (blue/green)
     */
    public BlueGreenDeployment switchFile() {
        BlueGreenDeployment switchTo = getInactiveFile();
        saveToStorageProvider(switchTo);
        return switchTo;
    }

    /**
     * Creates a new active file or updates it's contents
     *
     * @param blueGreen corresponds to {@link SwiftObject#getPayload()}
     * @return {@link SwiftObject#getETag()} of the object.
     */
    private String saveToStorageProvider(BlueGreenDeployment blueGreen) {
        Payload payload = new StringPayload(blueGreen.toString());
        return objectStorageProvider.put(activeFileName, payload);
    }

}
