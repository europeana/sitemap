package eu.europeana.sitemap;

import eu.europeana.features.S3ObjectStorageClient;
import eu.europeana.sitemap.service.ActiveDeploymentService;
import eu.europeana.sitemap.service.Deployment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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

    private static S3ObjectStorageClient mockStorage = mock(S3ObjectStorageClient.class);

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
    @Disabled
    public void testDeleteInactive() {
        ActiveDeploymentService ass = new ActiveDeploymentService(mockStorage);

        // files to delete
        String deleteFile1 = StorageFileName.getSitemapFileName(SitemapType.ENTITY, Deployment.BLUE, "1");
        String deleteFile2 = StorageFileName.getSitemapFileName(SitemapType.ENTITY, Deployment.BLUE, "2");
        String deleteFile3 = StorageFileName.getSitemapFileName(SitemapType.ENTITY, Deployment.BLUE, "3");
        mockStorage.putObject(deleteFile1, null);
        mockStorage.putObject(deleteFile2, null);
        mockStorage.putObject(deleteFile3, null);

        // 2 files to keep
        String keepFile1 = StorageFileName.getSitemapFileName(SitemapType.ENTITY, Deployment.GREEN, "1");
        String keepFile2 = StorageFileName.getSitemapFileName(SitemapType.RECORD, Deployment.BLUE, "1");
        mockStorage.putObject(keepFile1, null);
        mockStorage.putObject(keepFile2, null);

        // TODO FIX unit test, for some reason the listAll is not working properly because it's by-passing the mock and returning null
        assertEquals(5, mockStorage.listAll(null).getKeyCount());

        assertEquals(3, ass.deleteInactiveFiles(SitemapType.ENTITY));
        assertFalse(mockStorage.isObjectAvailable(deleteFile1));
        assertFalse(mockStorage.isObjectAvailable(deleteFile2));
        assertFalse(mockStorage.isObjectAvailable(deleteFile3));
        assertTrue(mockStorage.isObjectAvailable(keepFile1));
        assertTrue(mockStorage.isObjectAvailable(keepFile2));
    }

}
