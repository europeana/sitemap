package eu.europeana.sitemap.service;

import eu.europeana.s3.S3Object;
import eu.europeana.s3.S3ObjectStorageClient;
import eu.europeana.sitemap.Constants;
import eu.europeana.sitemap.SitemapType;
import eu.europeana.sitemap.StorageFileName;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;


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

    private static final String CONTENT_TYPE_DEPLOYMENT_FILE = "text/plain";

    private static final int PROGRESS_INTERVAL = 100;

    private final S3ObjectStorageClient objectStorageProvider;

    /**
     * Initialize the service
     * @param objectStorageClient storage where active deployment info is saved
     */
    @Autowired
    public ActiveDeploymentService(S3ObjectStorageClient objectStorageClient) {
        LOG.debug("Init");
        this.objectStorageProvider = objectStorageClient;
    }

    /**
     * Returns the active deployment (blue or green), depending on which value is in the active file. If no active file
     * is present we create a new one with the value "green"
     * @param sitemapType type of sitemap (record or entity)
     * @return deployment that is active for this type
     */
    public Deployment getActiveDeployment(SitemapType sitemapType) {
        Deployment result = null;
        String activeFileName = StorageFileName.getActiveDeploymentFileName(sitemapType);
        LOG.debug("Reading active file {}", activeFileName);

        try (S3Object s3Object = objectStorageProvider.getObject(activeFileName)) {
            if (s3Object == null) {
                // if the active file does not exist we create a new one
                LOG.error("File {} not present. Initializing new active deployment file...", activeFileName);
                saveToStorageProvider(Deployment.GREEN, activeFileName);
                result = Deployment.GREEN;
            } else {
                String blueGreen = new String(s3Object.inputStream().readAllBytes(), StandardCharsets.UTF_8);
                result = Deployment.fromString(blueGreen);
            }
        } catch (IOException e) {
            LOG.error("Error while processing the file {} to determine the current active site map", activeFileName, e);
        }
        LOG.debug("Returning deployment {}", result);
        return result;
    }

    /**
     * Returns the inactive deployment (blue or green), depending on which value is in the active file. If no active file
     * is present we create a new one with the value "green"
     * @param sitemapType type of sitemap (record or entity)
     * @return deployment that is active for this type
     */
    public Deployment getInactiveDeployment(SitemapType sitemapType) {
        if (Deployment.BLUE == getActiveDeployment(sitemapType)) {
            return Deployment.GREEN;
        }
        return Deployment.BLUE;
    }

    /**
     * Deletes all the inactive files for the provided type
     * @param sitemapType type of sitemap (record or entity)
     * @return the number of deleted files
     */
    public long deleteInactiveFiles(SitemapType sitemapType) {
        long result = 0;

        // determine inactive files
        Deployment inactive = this.getInactiveDeployment(sitemapType);
        String fileNameToDelete = StorageFileName.getSitemapFileName(sitemapType, inactive, null);
        // remove .xml extension so we delete also the index file
        fileNameToDelete = fileNameToDelete.split(Constants.XML_EXTENSION)[0];

        // list files
        String continuationToken = null;
        LOG.info("Deleting all old files with name starting with {} ...", fileNameToDelete);
        do {
            ListObjectsV2Response list = objectStorageProvider.listAll(continuationToken);
            continuationToken = list.nextContinuationToken();
            List<software.amazon.awssdk.services.s3.model.S3Object> results = list.contents();
            if (results.isEmpty()) {
                LOG.info("No files to remove.");
            } else {
                for (software.amazon.awssdk.services.s3.model.S3Object obj : results) {
                    result = deleteInactiveFile(obj.key(), fileNameToDelete, result);
                }
            }
        } while (continuationToken != null);

        LOG.info("Deleted {} old files", result);
        return result;
    }

    private long deleteInactiveFile(String fileName, String fileNameToDelete, long filesDeleted) {
        if (fileName.startsWith(fileNameToDelete)) {
            LOG.debug("Deleting file {}", fileName);
            objectStorageProvider.deleteObject(fileName);
            filesDeleted++;
            // report on progress
            if (filesDeleted > 0 && filesDeleted % PROGRESS_INTERVAL == 0) {
                LOG.info("Deleted {} old files", filesDeleted);
            }
        }
        return filesDeleted;
    }

    /**
     * Switch between blue/green deployment for the provided type
     * @param sitemapType type of sitemap (record or entity)
     * @return the now active deployment (blue/green)
     */
    public Deployment switchDeployment(SitemapType sitemapType) {
        Deployment switchTo = getInactiveDeployment(sitemapType);
        String activeFileName = StorageFileName.getActiveDeploymentFileName(sitemapType);
        saveToStorageProvider(switchTo, activeFileName);
        return switchTo;
    }

    /**
     * Creates a new active file or updates its contents
     * @param blueGreen deployment
     * @return eTag of the saved object.
     */
    private String saveToStorageProvider(Deployment blueGreen, String activeFileName) {
        LOG.debug("Saving value {} in file {} ", blueGreen, activeFileName);
        return objectStorageProvider.putObject(activeFileName, CONTENT_TYPE_DEPLOYMENT_FILE,
                blueGreen.toString().getBytes(StandardCharsets.UTF_8));
    }

}
