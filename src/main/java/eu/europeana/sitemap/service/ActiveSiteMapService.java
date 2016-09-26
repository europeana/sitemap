package eu.europeana.sitemap.service;

import eu.europeana.sitemap.swift.SwiftProvider;
import org.apache.commons.io.IOUtils;
import org.jclouds.io.Payload;
import org.jclouds.io.payloads.StringPayload;
import org.jclouds.openstack.swift.v1.domain.SwiftObject;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jeroen on 21-9-16.
 */
public class ActiveSiteMapService {
    private static final Logger log = Logger.getLogger(MongoSitemapService.class.getName());

    public static final String EUROPEANA_SITEMAP_HASHED_GREEN = "europeana-sitemap-hashed-green.xml";
    public static final String EUROPEANA_SITEMAP_HASHED_BLUE = "europeana-sitemap-hashed-blue.xml";
    public static final String EUROPEANA_ACTIVE_SITEMAP_CACHE_FILE = "europeana-sitemap-active-xml-file.txt";
    @Resource
    private SwiftProvider swiftProvider;


    public String getActiveFile() {
        String result = "";
        String activeSiteMapFile = EUROPEANA_ACTIVE_SITEMAP_CACHE_FILE;
        if (swiftProvider.getObjectApi().getWithoutBody(activeSiteMapFile) == null) {
            //In case that the active indication file does not exist, so we create one
            saveToSwift(EUROPEANA_SITEMAP_HASHED_GREEN);
            return EUROPEANA_SITEMAP_HASHED_GREEN;
        } else {
            try {
                StringWriter writer = new StringWriter();
                InputStream in = swiftProvider.getObjectApi().get(activeSiteMapFile).getPayload().openStream();
                IOUtils.copy(in, writer);
                result = writer.toString();
                in.close();

            } catch (IOException e) {
                log.log(Level.SEVERE, String.format("Error while processing the file: %s to determen the current active site map", activeSiteMapFile), e.getCause());

            }
        }
        return result;
    }

    public String getInactiveFile() {
        String result;
        if (getActiveFile().equals(EUROPEANA_SITEMAP_HASHED_GREEN)) {
            result = EUROPEANA_SITEMAP_HASHED_GREEN;
        } else {
            result = EUROPEANA_SITEMAP_HASHED_BLUE;
        }
        return result;

    }

    public void switchFile() {
        if (getActiveFile().equals(EUROPEANA_SITEMAP_HASHED_GREEN)) {
            saveToSwift(EUROPEANA_SITEMAP_HASHED_BLUE);
        } else {
            saveToSwift(EUROPEANA_SITEMAP_HASHED_GREEN);
        }
    }

    /**
     * Creates or updates a {@link SwiftObject}.
     *
     * @param value corresponds to {@link SwiftObject#getPayload()}
     * @return {@link SwiftObject#getETag()} of the object.
     */
    private String saveToSwift(String value) {
        Payload payload = new StringPayload(value);
        String result = swiftProvider.getObjectApi().put(EUROPEANA_ACTIVE_SITEMAP_CACHE_FILE, payload);
        return result;
    }


    public SwiftProvider getSwiftProvider() {
        return swiftProvider;
    }

    public void setSwiftProvider(SwiftProvider swiftProvider) {
        this.swiftProvider = swiftProvider;
    }
}
