package eu.europeana.sitemap;


/**
 * Enumeration of supported sitemap types (and accompanying base file name)
 *
 * @author Patrick Ehlert
 * Created on 12-06-2018
 */
public enum SitemapType {
    RECORD("record", Constants.SITEMAP_RECORD_FILENAME_BASE),
    ENTITY("entity", Constants.SITEMAP_ENTITY_FILENAME_BASE);

    private final String name;
    private final String fileNameBase;

    SitemapType(String name, String fileNameBase) {
        this.name = name;
        this.fileNameBase = fileNameBase;
    }

    @Override
    public String toString() {
        return this.name;
    }

    public String getFileNameBase() {
        return this.fileNameBase;
    }


}
