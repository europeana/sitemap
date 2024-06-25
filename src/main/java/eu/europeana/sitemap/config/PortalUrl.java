package eu.europeana.sitemap.config;

import eu.europeana.sitemap.Constants;
import eu.europeana.sitemap.SitemapType;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Generates the various portal urls that are publicly accessible. This includes record urls, entity urls as well as
 * sitemap file urls.
 */
@Configuration
@PropertySource("classpath:sitemap.properties")
@PropertySource(value = "classpath:sitemap.user.properties", ignoreResourceNotFound = true)
public class PortalUrl {

    private static final Logger LOG = LogManager.getLogger(PortalUrl.class);

    @Value("${portal.base.url}")
    private String portalBaseUrl;

    @Value("${record.portal.path}")
    private String recordPortalPath;
    @Value("${entity.portal.path}")
    private String entityPortalPath;

    /**
     * Return the public url of a sitemap index file
     * @param type sitemap type (record or entity)
     * @return the url of a public sitemap index file
     */
    public String getSitemapIndexUrl(SitemapType type) {
        return portalBaseUrl +
                Constants.PATH_SEPARATOR +
                type.getFileNameBase() +
                Constants.SITEMAP_INDEX_SUFFIX +
                Constants.XML_EXTENSION;
    }

    /**
     * Return the public url of a sitemap file (as it appears in the sitemap index file).
     *
     * @param baseUrl baseUrl used for generating the result
     * @param type sitemap type (record or entity)
     * @param appendix appendix of the file (e.g. ?from=0&to=45000)
     * @return the url of a public sitemap file
     */
    private static String getSitemapUrlPlain(String baseUrl, SitemapType type, String appendix) {
        return baseUrl +
                Constants.PATH_SEPARATOR +
                type.getFileNameBase() +
                Constants.XML_EXTENSION +
                appendix;
    }

    /**
     * Return the public url of a sitemap file (as it appears in the sitemap index file) but url encoded
     *
     * @param baseUrl baseUrl used for generating the result
     * @param type sitemap type (record or entity)
     * @param appendix appendix of the file (e.g. ?from=0&to=45000)
     * @return the url of a public sitemap file
     */
    public static String getSitemapUrlEncoded(String baseUrl, SitemapType type, String appendix) {
        return StringEscapeUtils.escapeXml10(getSitemapUrlPlain(baseUrl, type, appendix));
    }


    /**
     * Return the public url of a sitemap file (as it appears in the sitemap index file).
     *
     * @param type sitemap type (record or entity)
     * @param appendix appendix of the file (e.g. ?from=0&to=45000)
     * @return the url of a public sitemap file
     */
    protected String getSitemapUrlEncoded(SitemapType type, String appendix) {
        return getSitemapUrlEncoded(portalBaseUrl, type, appendix);
    }

    /**
     * Return a portal record page url
     * @param europeanaId CHO id of format /<datasetId>/<recordId>
     * @return portal record page url
     */
    public String getRecordUrl(String europeanaId) {
        return portalBaseUrl + recordPortalPath + europeanaId;
    }

    /**
     * Return the canonical (language-independent) portal entity page url. Note that portal will always redirect (301)
     * to a language-specific version of the page.
     * @param type entity type (either "agent" or "concept")
     * @param id entity id number, note that this is only unique within an entity type
     * @return canonical portal entity page url
     */
    public String getEntityUrl(String type, String id) {
        return portalBaseUrl +
                entityPortalPath +
                Constants.PATH_SEPARATOR +
                convertEntityTypeToPortalPath(type) +
                Constants.PATH_SEPARATOR +
                getEntityIdNumber(id);
    }

    /**
     * Return a language-specific portal entity page url (currently not used when generating sitemaps)
     * @param language page language
     * @param type entity type (either "agent" or "concept")
     * @param id entity id number, note that this is only unique within an entity type
     * @return a language-specific portal entity page url
     */
    public String getEntityUrl(String language, String type, String id) {
        return portalBaseUrl +
                Constants.PATH_SEPARATOR +
                language +
                entityPortalPath +
                Constants.PATH_SEPARATOR +
                convertEntityTypeToPortalPath(type) +
                Constants.PATH_SEPARATOR +
                getEntityIdNumber(id);
    }

    /**
     * @return String containing only the number of an entity id
     * (e.g. 23 when entityId = http://data.europeana.eu/agent/base/23)
     */
    private String getEntityIdNumber(String id) {
        return id.substring(id.lastIndexOf('/') + 1);
    }

    /**
     * Converts an entity type to the corresponding path used by Portal
     * @param type entity type
     * @return portal path name
     */
    private String convertEntityTypeToPortalPath(String type) {
        String result = null;
        if ("agent".equalsIgnoreCase(type)) {
            result = "person";
        } else if ("concept".equalsIgnoreCase(type)) {
            result = "topic";
        } else if ("timespan".equalsIgnoreCase(type)) {
            result = "time";
        } else if ("organization".equalsIgnoreCase(type)) {
            result = "organisation";
        }
        if (result == null) {
            LOG.warn("Unknown entity type '{}'", type);
        }
        return result;
    }

}
