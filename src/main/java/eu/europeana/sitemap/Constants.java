package eu.europeana.sitemap;

/**
 * Constants used for generating urls
 */
public final class Constants {

    public static final String XML_EXTENSION = ".xml";
    public static final String TXT_EXTENSION = ".txt";
    public static final char PATH_SEPARATOR = '/';
    public static final char DASH = '-';
    public static final String SITEMAP_RECORD_FILENAME_BASE = "sitemap-record";
    public static final String SITEMAP_ENTITY_FILENAME_BASE = "sitemap-entity";
    public static final String SITEMAP_INDEX_SUFFIX = "-index";
    public static final String SITEMAP_ACTIVE_DEPLOYMENT_SUFFIX = "-active";

    // MONGO Constants

    /** Used mongo fields **/
    public static final String ABOUT = "about";
    public static final String LASTUPDATED = "timestampUpdated";
    public static final String QUALITY_ANNOTATIONS_BODY = "qualityAnnotations.body";
    public static final String METADATA_TIER = "metadataTier";
    public static final String CONTENT_TIER = "contentTier";
    public static final int ITEMS_PER_SITEMAP_FILE = 45_000;

    public static final String PROJECT = "$project";
    public static final String ARRAY_ELEMENT_AT = "$arrayElemAt";
    public static final String MONGO_QA_BODY = "$" + QUALITY_ANNOTATIONS_BODY;
    public static final String SPLIT = "$split";
    public static final String MATCH = "$match";
    public static final String AND = "$and";
    public static final String IN = "$in";
    public static final String GTE = "$gte";

    private Constants() {
        // empty constructor to prevent initialization
    }
}
