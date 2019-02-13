package eu.europeana.sitemap.service.update;

import eu.europeana.features.ObjectStorageClient;
import eu.europeana.sitemap.MockObjectStorage;
import eu.europeana.sitemap.SitemapType;
import eu.europeana.sitemap.XmlUtils;
import eu.europeana.sitemap.service.Deployment;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Date;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 * Tests the SitemapGenerator class
 * @see SitemapGenerator
 *
 * @author Patrick Ehlert
 * Created on 05-06-2018
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class SitemapGeneratorTest {

    private static ObjectStorageClient mockStorage = mock(ObjectStorageClient.class);

    /**
     * Setup mock objectstorage
     */
    @BeforeClass
    public static void setup() {
        mockStorage = MockObjectStorage.setup(mockStorage);
    }

    /**
     * Clear mock storage before each test
     */
    @Before
    public void init() {
        MockObjectStorage.clear();
    }

    /**
     * Test the basic flow of the generator by generating 2 sitemap files and 1 index and checking contents
     */
    @Test
    public void testGenerator() {
        String websiteBaseUrl = "https://www.europeana.eu/portal";
        String fileName = SitemapType.ENTITY.getFileNameBase();
        Deployment deployment = Deployment.BLUE;

        String expectIndexFileName = fileName + "-" + deployment + "-index.xml";
        String expectSitemapFileName1 = fileName + "-" + deployment + ".xml?from=1&to=3";
        String expectSitemapFileName2 = fileName + "-" + deployment + ".xml?from=4&to=5";

        // storage should be empty
        assertFalse(mockStorage.isAvailable(expectIndexFileName));
        assertFalse(mockStorage.isAvailable(expectSitemapFileName1));
        assertFalse(mockStorage.isAvailable(expectSitemapFileName2));

        // generate the files (2 sitemap files, one with 3 items and one with 2 items, also 1 index file)
        int itemsPerSitemap = 3;
        SitemapGenerator generator = new SitemapGenerator(SitemapType.ENTITY, mockStorage);
        generator.init(Deployment.BLUE, websiteBaseUrl, itemsPerSitemap);
        for (int i = 1; i < (itemsPerSitemap * 2); i++) {
            generator.addItem(websiteBaseUrl + "/item/" + i + ".html", String.valueOf(i), new Date());
        }
        generator.finish();

        // storage should now contain the files
        assertTrue(expectIndexFileName, mockStorage.isAvailable(expectIndexFileName));
        assertTrue(expectSitemapFileName1, mockStorage.isAvailable(expectSitemapFileName1));
        assertTrue(expectSitemapFileName2, mockStorage.isAvailable(expectSitemapFileName2));

        // check if index refers to the generated files
        String indexContent =  XmlUtils.harmonizeXml(new String(mockStorage.getContent(expectIndexFileName)));
        assertEquals("Index file should only contain 2 references to sitemap files", 2, StringUtils.countMatches(indexContent, "<sitemap>"));
        String expectFileInIndex1 = websiteBaseUrl + "/" + fileName + ".xml?from=1&amp;to=3";
        String expectFileInIndex2 = websiteBaseUrl + "/" + fileName + ".xml?from=4&amp;to=5";
        assertTrue(indexContent.contains("<sitemap><loc>" + expectFileInIndex1 + "</loc></sitemap>"));
        assertTrue(indexContent.contains("<sitemap><loc>" + expectFileInIndex2 + "</loc></sitemap>"));

        // check sitemap file 1 contents
        String sitemap1Content = XmlUtils.harmonizeXml(new String(mockStorage.getContent(expectSitemapFileName1)));
        assertEquals("Sitemap file " + expectSitemapFileName1 + " should contain 3 items", 3, StringUtils.countMatches(sitemap1Content, "<url>"));
        for (int i = 1; i < 4; i++) {
            String itemLink = websiteBaseUrl + "/item/" + i + ".html";
            assertTrue("Sitemap should contain link "+itemLink, sitemap1Content.contains("<loc>" +itemLink+"</loc>"));
        }

        // check sitemap file 2 contents
        String sitemap2Content = XmlUtils.harmonizeXml(new String(mockStorage.getContent(expectSitemapFileName2)));
        assertEquals("Sitemap file " + expectSitemapFileName2 + " should contain 2 items", 2, StringUtils.countMatches(sitemap2Content, "<url>"));
        for (int i = 4; i < 5; i++) {
            String itemLink = websiteBaseUrl + "/item/" + i + ".html";
            assertTrue("Sitemap should contain link "+itemLink, sitemap2Content.contains("<loc>" +itemLink+"</loc>"));
        }
    }



    @Test(expected = IllegalStateException.class)
    public void testGenerateNotStarted1() {
        SitemapGenerator generator = new SitemapGenerator(SitemapType.RECORD, mockStorage);
        generator.addItem("http://some.item/1", null, null);
    }

    @Test(expected = IllegalStateException.class)
    public void testGenerateNotStarted2() {
        SitemapGenerator generator = new SitemapGenerator(SitemapType.RECORD, mockStorage);
        generator.finish();
    }

    @Test(expected = IllegalStateException.class)
    public void testGenerateStartTwice() {
        SitemapGenerator generator = new SitemapGenerator(SitemapType.ENTITY, mockStorage);
        generator.init(Deployment.BLUE, "https://www.fail.com", 5);
        generator.init(Deployment.GREEN, "https://www.fail.com", 5);
    }

    @Test(expected = IllegalStateException.class)
    public void testGenerateFinishTwice() {
        SitemapGenerator generator = new SitemapGenerator(SitemapType.ENTITY, mockStorage);
        generator.init(Deployment.BLUE, "https://www.fail.com", 5);
        generator.addItem("http://some.item/1", null, null);
        generator.finish();
        generator.finish();
    }

    @Test(expected = IllegalStateException.class)
    public void testGenerateAddAfterFinish() {
        SitemapGenerator generator = new SitemapGenerator(SitemapType.RECORD, mockStorage);
        generator.init(Deployment.GREEN, "https://www.fail.com", 7);
        generator.addItem("http://some.item/1", null, null);
        generator.finish();
        generator.addItem("http://some.item/2", null, null);
    }

}
