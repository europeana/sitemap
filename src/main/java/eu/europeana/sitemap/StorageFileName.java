package eu.europeana.sitemap;

import eu.europeana.sitemap.service.Deployment;
import org.apache.commons.lang3.StringUtils;

/**
 * Generates file names for internal usage (so with blue/green deployment)
 *
 * @author Patrick Ehlert on 2-1-18.
 */
public final class StorageFileName {

    private StorageFileName() {
        // empty constructor to prevent initialization
    }

    /**
     * Generates the name of the sitemap index file as it is (or should be) stored in the object storage
     * @param type sitemap type (record or entity)
     * @param blueGreen type of deployment (blue or green)
     * @return sitemap index file name
     */
    public static String getSitemapIndexFileName(SitemapType type, Deployment blueGreen) {
        StringBuilder sb = new StringBuilder(type.getFileNameBase())
                .append(Constants.DASH)
                .append(blueGreen)
                .append(Constants.SITEMAP_INDEX_SUFFIX)
                .append(Constants.XML_EXTENSION);
        return sb.toString();
    }

    /**
     * Generates the file name of a sitemap file as it is (or should be) stored in the object storage
     * @param type sitemap type (record or entity)
     * @param blueGreen deployment type (blue or green)
     * @param appendix appendix that is added to the file name (e.g. ?from=0&to=1000), can be null or empty
     * @return sitemap file name
     */
    public static String getSitemapFileName(SitemapType type, Deployment blueGreen, String appendix) {
        StringBuilder sb = new StringBuilder(type.getFileNameBase())
                .append(Constants.DASH)
                .append(blueGreen)
                .append(Constants.XML_EXTENSION);
        if (StringUtils.isNotEmpty(appendix)) {
            sb.append(appendix);
        }
        return sb.toString();
    }

    /**
     * Generate the name of the file containing the active deployment (blue/green).
     * @param type sitemap type (record or entity)
     * @return active deployment file
     */
    public static String getActiveDeploymentFileName(SitemapType type) {
        return type.getFileNameBase() + Constants.SITEMAP_ACTIVE_DEPLOYMENT_SUFFIX + Constants.TXT_EXTENSION;
    }


}
