package eu.europeana.sitemap;


import eu.europeana.sitemap.config.Constants;

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

    /**
     * @return the base name of files generated for this sitemap type
     */
    public String getFileNameBase() {
        return this.fileNameBase;
    }


}
