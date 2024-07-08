package eu.europeana.sitemap.config;

import eu.europeana.sitemap.SitemapType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestPropertySource("classpath:sitemap-test.properties")
@SpringBootTest(classes = {PortalUrl.class})
public class PortalUrlTest {

    @Autowired
    private PortalUrl fnp;

    private static final String PORTAL_BASE_URL = "https://www-test.eanadev.org";

    @Test
    public void testGetSitemapIndexUrl() {
        assertEquals(PORTAL_BASE_URL + "/sitemap-record-index.xml", fnp.getSitemapIndexUrl(SitemapType.RECORD));
        assertEquals(PORTAL_BASE_URL + "/sitemap-entity-index.xml", fnp.getSitemapIndexUrl(SitemapType.ENTITY));
    }

    @Test
    public void testGetSitemapUrl() {
        String appendix = "?from=0&to=10";
        String encoded_appendix = "?from=0&amp;to=10";
        assertEquals(PORTAL_BASE_URL + "/sitemap-record.xml" + encoded_appendix,
                fnp.getSitemapUrlEncoded(SitemapType.RECORD, appendix));
        assertEquals(PORTAL_BASE_URL + "/sitemap-entity.xml" + encoded_appendix,
                fnp.getSitemapUrlEncoded(SitemapType.ENTITY, appendix));
    }

    @Test
    public void testGetRecordUrl() {
        String europeanaId = "/92092/BibliographicResource_1000086018920";
        assertEquals(PORTAL_BASE_URL + "/record/92092/BibliographicResource_1000086018920",
                fnp.getRecordUrl(europeanaId));
    }

    @Test
    public void testGetOldEntityUrlCanonical() {
        String type1 = "Agent";
        String id1 = "http://data.europeana.eu/agent/base/312";
        assertEquals(PORTAL_BASE_URL + "/collections/person/312", fnp.getEntityUrl(type1, id1));

        String type2 = "Concept";
        String id2 = "http://data.europeana.eu/concept/base/999";
        assertEquals(PORTAL_BASE_URL + "/collections/topic/999", fnp.getEntityUrl(type2, id2));
    }

    @Test
    public void testGetNewEntityUrlCanonical() {
        String type1 = "Agent";
        String id1 = "http://data.europeana.eu/agent/312";
        assertEquals(PORTAL_BASE_URL + "/collections/person/312", fnp.getEntityUrl(type1, id1));

        String type2 = "Concept";
        String id2 = "http://data.europeana.eu/concept/999";
        assertEquals(PORTAL_BASE_URL + "/collections/topic/999", fnp.getEntityUrl(type2, id2));
    }

    @Test
    public void testGetEntityUrlLanguage() {
        String language1 = "en";
        String type1 = "Agent";
        String id1 = "http://data.europeana.eu/agent/base/11";
        String result1 = fnp.getEntityUrl(language1, type1, id1);
        assertEquals(PORTAL_BASE_URL + "/en/collections/person/11", result1);

        String language2 = "ru";
        String type2 = "Concept";
        String id2 = "http://data.europeana.eu/concept/base/664";
        String result2 = fnp.getEntityUrl(language2, type2, id2);
        assertEquals(PORTAL_BASE_URL + "/ru/collections/topic/664", result2);
    }
}
