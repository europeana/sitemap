package eu.europeana.sitemap.config;

import eu.europeana.sitemap.SitemapType;
import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates the various portal urls that are publicly accessible. This includes record urls, entity urls as well as
 * sitemap file urls.
 */
@Configuration
@RefreshScope
@PropertySource("classpath:sitemap.properties")
@PropertySource(value = "classpath:sitemap.user.properties", ignoreResourceNotFound = true)
public class PortalUrl {

    private static final Pattern CHAR_NUMBER_OR_DASH = Pattern.compile("[^-a-zA-Z0-9]");
    private static final String PORTAL = "/portal";

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
        return portalBaseUrl + Constants.PATH_SEPARATOR + type.getFileNameBase() + Constants.SITEMAP_INDEX_SUFFIX
                + Constants.XML_EXTENSION;
    }

    /**
     * Return the public url of a sitemap file (as it appears in the sitemap index file).
     *
     * @param type sitemap type (record or entity)
     * @param appendix appendix of the file (e.g. ?from=0&to=45000)
     * @return the url of a public sitemap file
     */
    public String getSitemapUrl(SitemapType type, String appendix) {
        return PortalUrl.getSitemapUrl(portalBaseUrl, type, appendix, true);
    }

    /**
     * Return the public url of a sitemap file (as it appears in the sitemap index file).
     *
     * @param baseUrl baseUrl used for generating the result
     * @param type sitemap type (record or entity)
     * @param appendix appendix of the file (e.g. ?from=0&to=45000)
     * @param urlEncode if true then the result is urlencoded, if false no url-encoding is applied
     * @return the url of a public sitemap file
     */
    public static String getSitemapUrl(String baseUrl, SitemapType type, String appendix, boolean urlEncode) {
        String result = baseUrl + Constants.PATH_SEPARATOR + type.getFileNameBase()  +
                Constants.XML_EXTENSION + appendix;
        if (urlEncode) {
            return StringEscapeUtils.escapeXml(result);
        }
        return result;
    }

    /**
     * Return a portal record page url
     * @param europeanaId CHO id of format /<datasetId>/<recordId>
     * @return portal record page url
     */
    public String getRecordUrl(String europeanaId) {
        return portalBaseUrl + recordPortalPath + europeanaId + Constants.HTML_EXTENSION;
    }

    /**
     * Return the canonical (language-independent) portal entity page url. Note that portal will always redirect (301)
     * to a language-specific version of the page.
     * @param type entity type (either "agent" or "concept")
     * @param id entity id number, note that this is only unique within an entity type
     * @return canonical portal entity page url
     */
    public String getEntityUrl(String type, String id) {
        return portalBaseUrl + entityPortalPath + Constants.PATH_SEPARATOR + convertEntityTypeToPortalPath(type) +
                Constants.PATH_SEPARATOR + id;
    }

    /**
     * Return a language-specific portal entity page url
     * @param language page language
     * @param type entity type (either "agent" or "concept")
     * @param id entity id number, note that this is only unique within an entity type
     * @param prefLabel english preflabel of the entity, can be empty or null
     * @return a language-specific portal entity page url
     */
    public String getEntityUrl(String language, String type, String id, String prefLabel) {
        StringBuilder s = new StringBuilder(portalBaseUrl);
        if (entityPortalPath.startsWith(PORTAL)) {
            s.append(PORTAL).
                    append(Constants.PATH_SEPARATOR).
                    append(language).
                    append(entityPortalPath.substring(entityPortalPath.lastIndexOf(PORTAL) + PORTAL.length()));
        } else {
            s.append(Constants.PATH_SEPARATOR).
                    append(language).
                    append(entityPortalPath);
        }
        s.append(Constants.PATH_SEPARATOR).
                append(convertEntityTypeToPortalPath(type)).
                append(Constants.PATH_SEPARATOR).
                append(convertEntityIdPrefLabelToPortalFile(id, prefLabel));
        return s.toString();
    }

    /**
     * Converts an entity type to the corresponding path used by Portal
     * @param type entity type
     * @return portal path name
     */
    private String convertEntityTypeToPortalPath(String type) {
        if ("agent".equalsIgnoreCase(type)) {
            return "people";
        }
        if ("concept".equalsIgnoreCase(type)) {
            return "topics";
        }
        return null;
    }

    /**
     * Converts an entity id number and English(!) preflabel to a html file name as used by Portal. The idea is that
     * by generating the precise Portal url we'll prevent including urls in the sitemap that redirect somewhere else (at
     * least as much as possible).
     *
     * @param id entity id number, note that this is only unique with an entity type
     * @param prefLabel, english preflabel of the entity, can be empty or null
     * @return html file name as used by Portal
     */
    private String convertEntityIdPrefLabelToPortalFile(String id, String prefLabel) {
        // we assume id is never empty
        String result = id;
        if (!StringUtils.isEmpty(prefLabel)) {
            result = result + '-' + convertPrefLabel(prefLabel);
        }
        return result + Constants.HTML_EXTENSION;
    }

    /**
     * This should return the same results as the Ruby library used by Portal which is https://github.com/rsl/stringex
     */
    private String convertPrefLabel(String prefLabel) {
        String result = prefLabel.replaceAll("\\s", "-").replaceAll("&", "and");

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
