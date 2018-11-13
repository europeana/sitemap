package eu.europeana.sitemap.service;

import eu.europeana.sitemap.FileNames;

/**
 * @author Patrick Ehlert
 * Created on 12-06-2018
 */
public enum SitemapType {
    RECORD("record", FileNames.SITEMAP_RECORD_INDEX_FILE),
    ENTITY("entity", FileNames.SITEMAP_ENTITY_INDEX_FILE);

    private final String name;
    private final String indexFileName;

    private SitemapType(String name, String indexFileName) {
        this.name = name;
        this.indexFileName = getIndexFileName();
    }

    public String toString() {
        return this.name;
    }

    public String getIndexFileName() {
        return this.indexFileName;
    }

}
