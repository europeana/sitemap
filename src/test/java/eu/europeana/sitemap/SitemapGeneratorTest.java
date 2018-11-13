package eu.europeana.sitemap;

import eu.europeana.domain.StorageObject;
import eu.europeana.features.ObjectStorageClient;
import eu.europeana.sitemap.service.ActiveSitemapService;
import eu.europeana.sitemap.service.BlueGreenDeployment;
import eu.europeana.sitemap.service.SitemapGenerator;
import org.apache.commons.lang.StringUtils;
import org.jclouds.io.Payload;
import org.jclouds.io.payloads.ByteArrayPayload;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the SitemapGenerator class
 * @author Patrick Ehlert
 * Created on 05-06-2018
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class SitemapGeneratorTest {

    private static ObjectStorageClient mockStorage = mock(ObjectStorageClient.class);
    private static Map<String, Payload> storageMap = new HashMap<>();

    /**
     * Setup mock objectstorage and activeSitemap with all methods that are used during testing.
     * Note that we simulate that the eTag is the same as the file name
     */
    @BeforeClass
    public static void setup() {
        when(mockStorage.put(anyString(),any())).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                String fileName = (String) args[0];
                Payload payload = (Payload) args[1];
                storageMap.put(fileName, payload);
                return fileName;
            }
        });
        when(mockStorage.isAvailable(anyString())).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                String fileName = (String) args[0];
                return storageMap.keySet().contains(fileName);
            }
        });
        when(mockStorage.getContent(anyString())).thenAnswer(new Answer<byte[]>() {
            @Override
            public byte[] answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                String fileName = (String) args[0];
                Payload result = storageMap.get(fileName);
                if (result != null) {
                    return ((ByteArrayPayload) result).getRawContent();
                }
                return null;
            }
        });
        when(mockStorage.list()).thenAnswer(new Answer<List<StorageObject>>() {
            @Override
            public List<StorageObject> answer(InvocationOnMock invocation) throws Throwable {
                List<StorageObject> result = new ArrayList<>();
                for (Map.Entry<String, Payload> entry : storageMap.entrySet()) {
                    result.add(new StorageObject(entry.getKey(), null, null, entry.getValue()));
                }
                return result;
            }
        });
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                String fileName = (String) invocation.getArguments()[0];
                storageMap.remove(fileName);
                return null;
            }
        }).when(mockStorage).delete(anyString());
    }

    /**
     * Clear mock storage before each test
     */
    @Before
    public void init() {
        storageMap.clear();
    }

    /**
     * Test the basic flow of the generator by generating 2 sitemap files and 1 index and checking contents
     */
    @Test
    public void testGenerator() {
        String websiteBaseUrl = "https://www.test.com";
        String fileName = "sitemap-test";
        BlueGreenDeployment blueGreen = BlueGreenDeployment.BLUE;

        String indexFileName = fileName + "-index.xml";
        String sitemapFileName1 = fileName + "-" + blueGreen + ".xml?from=1&to=3";
        String sitemapFileName2 = fileName + "-" + blueGreen + ".xml?from=4&to=5";
        String fileInIndex1 = websiteBaseUrl + "/" + fileName + ".xml?from=1&amp;to=3";
        String fileInIndex2 = websiteBaseUrl + "/" + fileName + ".xml?from=4&amp;to=5";

        // storage should be empty
        assertFalse(mockStorage.isAvailable(indexFileName));
        assertFalse(mockStorage.isAvailable(sitemapFileName1));
        assertFalse(mockStorage.isAvailable(sitemapFileName2));

        // generate the files
        int itemsPerSitemap = 3;
        SitemapGenerator generator = new SitemapGenerator(mockStorage, blueGreen, websiteBaseUrl, fileName, itemsPerSitemap);
        for (int i = 1; i < (itemsPerSitemap*2); i++) {
            generator.addItem(websiteBaseUrl + "/item/" + i + ".html", String.valueOf(i), new Date());
        }
        generator.finish();

        // storage should now contain the files
        assertTrue(mockStorage.isAvailable(indexFileName));
        assertTrue(mockStorage.isAvailable(sitemapFileName1));
        assertTrue(mockStorage.isAvailable(sitemapFileName2));

        // check if index refers to the generated files
        String indexContent =  harmonizeXml(new String(mockStorage.getContent(indexFileName)));
        assertEquals("Index file should only contain 2 references to sitemap files", 2, StringUtils.countMatches(indexContent, "<sitemap>"));
        assertTrue(indexContent.contains("<sitemap><loc>" + fileInIndex1 + "</loc></sitemap>"));
        assertTrue(indexContent.contains("<sitemap><loc>" + fileInIndex2 + "</loc></sitemap>"));

        // check sitemap file 1 contents
        String sitemap1Content = harmonizeXml(new String(mockStorage.getContent(sitemapFileName1)));
        assertEquals("Sitemap file " + sitemapFileName1 + " should contain 3 items", 3, StringUtils.countMatches(sitemap1Content, "<url>"));
        for (int i = 1; i < 4; i++) {
            String itemLink = websiteBaseUrl + "/item/" + i + ".html";
            assertTrue("Sitemap should contain link "+itemLink, sitemap1Content.contains("<loc>" +itemLink+"</loc>"));
        }

        // check sitemap file 2 contents
        String sitemap2Content = harmonizeXml(new String(mockStorage.getContent(sitemapFileName2)));
        assertEquals("Sitemap file " + sitemapFileName2 + " should contain 2 items", 2, StringUtils.countMatches(sitemap2Content, "<url>"));
        for (int i = 4; i < 5; i++) {
            String itemLink = websiteBaseUrl + "/item/" + i + ".html";
            assertTrue("Sitemap should contain link "+itemLink, sitemap2Content.contains("<loc>" +itemLink+"</loc>"));
        }
    }

    // remove all newlines, tabs and multiple spacing so we return 1 single line of xml for easy 'contains' checks
    private String harmonizeXml(String xml) {
        return xml.replaceAll("[\\n\\r\\t]", "").replaceAll("\\s+", " ").trim().toLowerCase(Locale.getDefault());
    }

}
