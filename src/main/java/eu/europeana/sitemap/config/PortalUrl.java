package eu.europeana.sitemap.config;

import eu.europeana.sitemap.Constants;
import eu.europeana.sitemap.SitemapType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates the various portal urls that are publicly accessible. This includes record urls, entity urls as well as
 * sitemap file urls.
 */
@Configuration
@PropertySource("classpath:sitemap.properties")
@PropertySource(value = "classpath:sitemap.user.properties", ignoreResourceNotFound = true)
public class PortalUrl {

    private static final Logger LOG = LogManager.getLogger(PortalUrl.class);

    private static final Pattern CHAR_NUMBER_OR_DASH = Pattern.compile("[^-a-zA-Z0-9]");

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
        StringBuilder s = new StringBuilder(portalBaseUrl)
                .append(Constants.PATH_SEPARATOR)
                .append(type.getFileNameBase())
                .append(Constants.SITEMAP_INDEX_SUFFIX)
                .append(Constants.XML_EXTENSION);
        return s.toString();
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
        StringBuilder s = new StringBuilder(baseUrl)
                .append(Constants.PATH_SEPARATOR)
                .append(type.getFileNameBase())
                .append(Constants.XML_EXTENSION)
                .append(appendix);
        return s.toString();
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
        StringBuilder s = new StringBuilder(portalBaseUrl)
                .append(entityPortalPath)
                .append(Constants.PATH_SEPARATOR)
                .append(convertEntityTypeToPortalPath(type))
                .append(Constants.PATH_SEPARATOR)
                .append(getEntityIdNumber(id));
        return s.toString();
    }

    /**
     * Return a language-specific portal entity page url (currently not used when generating sitemaps)
     * @param language page language
     * @param type entity type (either "agent" or "concept")
     * @param id entity id number, note that this is only unique within an entity type
     * @param prefLabel english preflabel of the entity, can be empty or null
     * @return a language-specific portal entity page url
     */
    public String getEntityUrl(String language, String type, String id, String prefLabel) {
        StringBuilder s = new StringBuilder(portalBaseUrl);
        s.append(Constants.PATH_SEPARATOR).
                    append(language).
                    append(entityPortalPath).
                    append(Constants.PATH_SEPARATOR).
                    append(convertEntityTypeToPortalPath(type)).
                    append(Constants.PATH_SEPARATOR).
                    append(getEntityIdNumber(id));
        if (!StringUtils.isEmpty(prefLabel)) {
            s.append('-');
            s.append(convertPrefLabel(prefLabel));
        }
        return s.toString();
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

    /**
     * Converts an English(!) preflabel to a String as used by Portal in entity-urls. The idea is that by generating the
     * precise Portal url we'll prevent including urls in the sitemap that redirect somewhere else (at least as much as
     * possible).
     *
     * @deprecated March 2020: This should return the same results as the Ruby library used by Portal which is https://github.com/rsl/stringex
     * but sadly it doesn't in all cases, so at the moment this code is not used.
     *
     */
    @Deprecated(since = "March 2020")
    private String convertPrefLabel(String prefLabel) {
        String result = prefLabel.replaceAll("\\s", "-").replace("&", "and");

        // strip everything that's not a normal character, number or dash
        Matcher matcher = CHAR_NUMBER_OR_DASH.matcher(result);
        return matcher.replaceAll("").toLowerCase(Locale.GERMAN);

        // TODO note that this method doesn't generate the correct url for several entities, including but not limited to:
        // http://data.europeana.eu/concept/base/443 (name is "Pilón")
        // http://data.europeana.eu/concept/base/457 (name is "Volkstümliche Musik")
        // http://data.europeana.eu/concept/base/521 (name is "Batá-rumba")
        // http://data.europeana.eu/concept/base/488 (name is "Nòva cançon")
        // http://data.europeana.eu/agent/base/34859 (name is "The B.G.'z")
        // However since Portal will always redirect to the correct url if we get it wrong we'll let this be for now
    }




}
