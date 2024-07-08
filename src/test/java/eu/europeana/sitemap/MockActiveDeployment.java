package eu.europeana.sitemap;

import eu.europeana.sitemap.service.ActiveDeploymentService;
import eu.europeana.sitemap.service.Deployment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mockito.stubbing.Answer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Simple mock of the ActiveDeploymentService.
 * @see ActiveDeploymentService
 *
 * Note that it doesn't use any underlying objectstorage and assumes there is only 1 sitemap type (so do not use
 * for testing multiple sitemaps types).
 */
public class MockActiveDeployment {

    private static final Logger LOG = LogManager.getLogger(MockActiveDeployment.class);

    private static Deployment active = Deployment.GREEN;

    public static ActiveDeploymentService setup(ActiveDeploymentService mockActiveDeployment) {
        when(mockActiveDeployment.getActiveDeployment(any())).thenAnswer((Answer<Deployment>) invocation -> active);
        when(mockActiveDeployment.getInactiveDeployment(any())).thenAnswer((Answer<Deployment>) invocation -> {
            if (Deployment.GREEN.equals(active)) {
                return Deployment.BLUE;
            }
            return Deployment.GREEN;
        });
        when(mockActiveDeployment.switchDeployment(any())).thenAnswer((Answer<Deployment>) invocation -> {
            if (Deployment.GREEN.equals(active)) {
                return Deployment.BLUE;
            }
            return Deployment.GREEN;
        });
        when(mockActiveDeployment.deleteInactiveFiles(any())).thenAnswer((Answer<Long>) invocation -> {
            LOG.info("No inactive files to delete");
            return 0l;
        });

        return mockActiveDeployment;
    }
}
