package eu.europeana.sitemap;

import eu.europeana.sitemap.service.Deployment;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StorageFileNameTest {

    @Test
    public void testGetSitemapIndexFileName() {
        assertEquals("sitemap-record-blue-index.xml",
                StorageFileName.getSitemapIndexFileName(SitemapType.RECORD, Deployment.BLUE));
        assertEquals("sitemap-entity-green-index.xml",
                StorageFileName.getSitemapIndexFileName(SitemapType.ENTITY, Deployment.GREEN));
    }

    @Test
    public void getTestGetSitemapIndexFileName() {
        String appendix = "?from=10&to=20";
        assertEquals("sitemap-record-green.xml?from=10&to=20",
                StorageFileName.getSitemapFileName(SitemapType.RECORD, Deployment.GREEN, appendix));
        assertEquals("sitemap-entity-blue.xml",
                StorageFileName.getSitemapFileName(SitemapType.ENTITY, Deployment.BLUE, null));
    }

    @Test
    public void testGetActiveDeploymentFileName() {
        assertEquals("sitemap-record-active.txt", StorageFileName.getActiveDeploymentFileName(SitemapType.RECORD));
        assertEquals("sitemap-entity-active.txt", StorageFileName.getActiveDeploymentFileName(SitemapType.ENTITY));
    }



}
