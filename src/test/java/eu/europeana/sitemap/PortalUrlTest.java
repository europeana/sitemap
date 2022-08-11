package eu.europeana.sitemap;

import eu.europeana.sitemap.config.PortalUrl;
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
        String prefLabel1 = "Frederic Leighton, 1st Baron Leighton";
        String result1 = fnp.getEntityUrl(language1, type1, id1, prefLabel1);
        assertEquals(PORTAL_BASE_URL + "/en/collections/person/11-frederic-leighton-1st-baron-leighton", result1);

        String language2 = "ru";
        String type2 = "Concept";
        String id2 = "http://data.europeana.eu/concept/base/664";
        String prefLabel2 = null;
        String result2 = fnp.getEntityUrl(language2, type2, id2, prefLabel2);
        assertEquals(PORTAL_BASE_URL + "/ru/collections/topic/664", result2);

        String language3 = "it";
        String type3 = "Agent";
        String id3 = "http://data.europeana.eu/agent/base/93590";
        String prefLabel3 = "W. J. Gruffydd (Elerydd)";
        String result3 = fnp.getEntityUrl(language3, type3, id3, prefLabel3);
        assertEquals(PORTAL_BASE_URL + "/it/collections/person/93590-w-j-gruffydd-elerydd", result3);
    }
}
