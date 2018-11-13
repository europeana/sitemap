package eu.europeana.sitemap;

import eu.europeana.sitemap.service.ActiveSitemapService;
import eu.europeana.sitemap.service.BlueGreenDeployment;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the ActiveSitemapService
 *
 * @author Patrick Ehlert
 * Created on 11-06-2018
 */
public class ActiveSitemapServiceTest {

    private static ActiveSitemapService mockActive = mock(ActiveSitemapService.class);
    private static BlueGreenDeployment active = BlueGreenDeployment.GREEN;

    @BeforeClass
    public static void setup() {
        when(mockActive.switchFile()).thenAnswer(new Answer<BlueGreenDeployment>() {
            @Override
            public BlueGreenDeployment answer(InvocationOnMock invocation) throws Throwable {
                if (BlueGreenDeployment.GREEN.equals(active)) {
                    active = BlueGreenDeployment.BLUE;
                } else {
                    active = BlueGreenDeployment.GREEN;
                }
                return active;
            }
        });
        when(mockActive.getInactiveFile()).then(new Answer<BlueGreenDeployment>() {
            @Override
            public BlueGreenDeployment answer(InvocationOnMock invocation) throws Throwable {
                if (BlueGreenDeployment.GREEN.equals(active)) {
                    return BlueGreenDeployment.BLUE;
                }
                return BlueGreenDeployment.GREEN;
            }
        });
    }

}
