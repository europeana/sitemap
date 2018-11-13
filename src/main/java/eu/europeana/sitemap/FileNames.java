package eu.europeana.sitemap;

import eu.europeana.sitemap.service.BlueGreenDeployment;
import org.apache.commons.lang.StringEscapeUtils;

/**
 * Shared constants such as sitemap file (base) names
 *
 * @author Patrick Ehlert on 2-1-18.
 */
public class FileNames {

    public static final String SITEMAP_RECORD_INDEX_FILE = "sitemap-record-index.xml";
    public static final String SITEMAP_RECORD_ACTIVE_FILE = "sitemap-record-bluegreen-active.txt";
    public static final String SITEMAP_RECORD_FILENAME_BASE = "sitemap-record";

    public static final String SITEMAP_ENTITY_INDEX_FILE = "sitemap-entity-index.xml";
    public static final String SITEMAP_ENTITY_ACTIVE_FILE = "sitemap-entity-bluegreen-active.txt";
    public static final String SITEMAP_ENTITY_FILENAME_BASE = "sitemap-entity";

    public static final String XML_EXTENSION = ".xml";
    public static final String TXT_EXTENSION = ".txt";

    private FileNames() {
        // empty constructor to avoid initialization
    }

    /**
     *
     * @param fileNameBase
     * @param blueGreen
     * @param appendix
     * @return the file name of a sitemap file as it is stored in the object storage
     */
    public static String getSitemapFileNameStorage(String fileNameBase, BlueGreenDeployment blueGreen, String appendix) {
        return addHyphenIfNeeded(fileNameBase) + blueGreen.toString() + XML_EXTENSION + appendix;
    }

    /**
     *
     * @param websiteBaseUrl
     * @param fileNameBase
     * @param appendix
     * @return the url of a sitemap file as it appears in the index file. A controller will check the active deployment
     * (blue/green) and convert this to the real sitemap file name as it is in the object storage
     */
    public static String getSitemapFileNameInIndex(String websiteBaseUrl, String fileNameBase, String appendix) {
        return StringEscapeUtils.escapeXml(websiteBaseUrl +"/" + fileNameBase  + XML_EXTENSION + appendix);
    }

    /**
     * Returns the name of the sitemap index file. Note that for indexes we do not use blue/green deployment
     * @param fileNameBase
     * @return
     */
    public static String getSitemapIndexFileName(String fileNameBase) {
        return addHyphenIfNeeded(fileNameBase) + "index" + XML_EXTENSION;
    }

    /**
     * Returns the name of the file containing the active deployment (blue/green).
     * @param fileNameBase
     * @return
     */
    public static String getActiveDeploymentFileName(String fileNameBase) {
        return addHyphenIfNeeded(fileNameBase) + "active" + TXT_EXTENSION;
    }

    private static String addHyphenIfNeeded(String fileNameBase) {
        if (fileNameBase.endsWith("-") || fileNameBase.endsWith("_")) {
            return fileNameBase;
        }
        return fileNameBase + "-";
    }
}
