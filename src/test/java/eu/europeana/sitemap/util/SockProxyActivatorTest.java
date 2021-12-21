package eu.europeana.sitemap.util;

import eu.europeana.sitemap.config.SocksProxyConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Test if activating usage of a sock proxy works fine
 */
public class SockProxyActivatorTest {

    @Test
    public void testPropertiesNotPresent() {
        assertFalse(SocksProxyActivator.activate(new SocksProxyConfig("notpresent.properties")));
    }

    @Test
    public void testPropertiesDisabled() {
        assertFalse(SocksProxyActivator.activate(new SocksProxyConfig("socks_config_disabled.properties")));
    }

    @Test
    public void testPropertiesEnabled() {
        assertTrue(SocksProxyActivator.activate(new SocksProxyConfig("socks_config_enabled.properties")));
    }
}
