package eu.europeana.sitemap;

import eu.europeana.features.ObjectStorageClient;
import eu.europeana.sitemap.service.ActiveDeploymentService;
import eu.europeana.sitemap.service.Deployment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Tests the ActiveDeploymentService
 * @see ActiveDeploymentService
 *
 * @author Patrick Ehlert
 * Created on 28-01-2019
 */
public class ActiveDeploymentServiceTest {

    private static ObjectStorageClient mockStorage = mock(ObjectStorageClient.class);

    /**
     * Setup mock objectstorage
     */
    @BeforeAll
    public static void setup() {
        mockStorage = MockObjectStorage.setup(mockStorage);
    }

    /**
     * Clear mock storage before each test
     */
    @BeforeEach
    public void init() {
        MockObjectStorage.clear();
    }

    @Test
    public void testActiveInactive() {
        ActiveDeploymentService ass = new ActiveDeploymentService(mockStorage);
        // default green is first active when initializing
        assertEquals(Deployment.GREEN, ass.getActiveDeployment(SitemapType.RECORD));
        assertEquals(Deployment.BLUE, ass.getInactiveDeployment(SitemapType.RECORD));
    }

    @Test
    public void testSwitch() {
        ActiveDeploymentService ass = new ActiveDeploymentService(mockStorage);
        Deployment active = ass.getActiveDeployment(SitemapType.ENTITY);
        if (Deployment.GREEN.equals(active)) {
            assertEquals(Deployment.BLUE, ass.switchDeployment(SitemapType.ENTITY));
            assertEquals(Deployment.GREEN, ass.switchDeployment(SitemapType.ENTITY));
        } else {
            assertEquals(Deployment.GREEN, ass.switchDeployment(SitemapType.ENTITY));
            assertEquals(Deployment.BLUE, ass.switchDeployment(SitemapType.ENTITY));
        }
    }

    @Test
    public void testDeleteInactive() {
        ActiveDeploymentService ass = new ActiveDeploymentService(mockStorage);

        // files to delete
        String deleteFile1 = StorageFileName.getSitemapFileName(SitemapType.ENTITY, Deployment.BLUE, "1");
        String deleteFile2 = StorageFileName.getSitemapFileName(SitemapType.ENTITY, Deployment.BLUE, "2");
        String deleteFile3 = StorageFileName.getSitemapFileName(SitemapType.ENTITY, Deployment.BLUE, "3");
        mockStorage.put(deleteFile1, null);
        mockStorage.put(deleteFile2, null);
        mockStorage.put(deleteFile3, null);

        // 2 files to keep
        String keepFile1 = StorageFileName.getSitemapFileName(SitemapType.ENTITY, Deployment.GREEN, "1");
        String keepFile2 = StorageFileName.getSitemapFileName(SitemapType.RECORD, Deployment.BLUE, "1");
        mockStorage.put(keepFile1, null);
        mockStorage.put(keepFile2, null);

        assertEquals(3, ass.deleteInactiveFiles(SitemapType.ENTITY));
        assertFalse(mockStorage.isAvailable(deleteFile1));
        assertFalse(mockStorage.isAvailable(deleteFile2));
        assertFalse(mockStorage.isAvailable(deleteFile3));
        assertTrue(mockStorage.isAvailable(keepFile1));
        assertTrue(mockStorage.isAvailable(keepFile2));
    }

}
