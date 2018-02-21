package eu.europeana.sitemap.service;

import eu.europeana.domain.StorageObject;
import eu.europeana.features.ObjectStorageClient;
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
import java.util.Optional;


/**
 * Check whether we use green or blue deployment
 * Created by jeroen on 21-9-16.
 */
@Service
public class ActiveSiteMapService {

    private static final Logger LOG = LogManager.getLogger(ActiveSiteMapService.class);

    public static final String EUROPEANA_SITEMAP_HASHED_GREEN = "europeana-sitemap-hashed-green.xml";
    public static final String EUROPEANA_SITEMAP_HASHED_BLUE = "europeana-sitemap-hashed-blue.xml";
    public static final String EUROPEANA_ACTIVE_SITEMAP_SWITCH_FILE = "europeana-sitemap-active-xml-file.txt";

    private final ObjectStorageClient objectStorageProvider;
    
    public ActiveSiteMapService(ObjectStorageClient objectStorageClient) {
        this.objectStorageProvider = objectStorageClient;
    }

    /**
     *
     * @return either the green or blue version of a sitemap file name. If there was an error or no active file was present
     * we return green
     */
    public String getActiveFile() {
        String result = "";
        String activeSiteMapFile = EUROPEANA_ACTIVE_SITEMAP_SWITCH_FILE;
        Optional<StorageObject> withoutBody = objectStorageProvider.getWithoutBody(activeSiteMapFile);
        StorageObject storageObjectValue = null;

        if (withoutBody.isPresent()) {
            StringWriter writer = new StringWriter();
            Optional<StorageObject> storageObject = objectStorageProvider.get(activeSiteMapFile);
            if (storageObject.isPresent()) {
                storageObjectValue = storageObject.get();
                try (InputStream in = storageObjectValue.getPayload().openStream()) {
                    IOUtils.copy(in, writer);
                    result = writer.toString();
                    storageObjectValue.getPayload().close();
                } catch (IOException e) {
                    LOG.error("Error while processing the file {} to determine the current active site map", activeSiteMapFile, e);
                }
            } else {
                LOG.error("Active file not present!");
            }
        } else {
            // In case that the active indication file does not exist, so we create one
            saveToStorageProvider(EUROPEANA_SITEMAP_HASHED_GREEN);
            result = EUROPEANA_SITEMAP_HASHED_GREEN;
        }
        return result;
    }

    /**
     *
     * @return the inactive sitemap file name (blue/green)
     */
    public String getInactiveFile() {
        String result;
        if (EUROPEANA_SITEMAP_HASHED_GREEN.equals(getActiveFile())) {
            result = EUROPEANA_SITEMAP_HASHED_BLUE;
        } else {
            result = EUROPEANA_SITEMAP_HASHED_GREEN;
        }
        return result;

    }

    /**
     * Switch between blue/green sitemap files
     * @return the now active sitemap file name (blue/green)
     */
    public String switchFile() {
        String result = getInactiveFile();
        saveToStorageProvider(result);
        return result;
    }

    /**
     * Creates or updates a {@link SwiftObject}.
     *
     * @param value corresponds to {@link SwiftObject#getPayload()}
     * @return {@link SwiftObject#getETag()} of the object.
     */
    private String saveToStorageProvider(String value) {
        Payload payload = new StringPayload(value);
        return objectStorageProvider.put(EUROPEANA_ACTIVE_SITEMAP_SWITCH_FILE, payload);
    }

}
