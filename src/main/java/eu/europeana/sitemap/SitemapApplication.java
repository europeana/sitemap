package eu.europeana.sitemap;

import eu.europeana.sitemap.config.SocksProxyConfig;
import eu.europeana.sitemap.util.SocksProxyActivator;
import org.apache.logging.log4j.LogManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.PropertySource;

/**
 * Main application and configuration
 * @author Patrick Ehlert on 14-11-17.
 */
@SpringBootApplication(exclude={MongoAutoConfiguration.class})
@PropertySource("classpath:build.properties")
public class SitemapApplication {

    /**
     * This method is called when starting as a Spring-Boot application (e.g. from your IDE)
     * @param args main application arguments
     */
    @SuppressWarnings("squid:S2095") // to avoid sonarqube false positive (see https://stackoverflow.com/a/37073154/741249)
    public static void main(String[] args)  {
        // When deploying to Cloud Foundry, this will log the instance index number, IP and GUID
        LogManager.getLogger(SitemapApplication.class).
                info("CF_INSTANCE_INDEX  = {}, CF_INSTANCE_GUID = {}, CF_INSTANCE_IP  = {}",
                        System.getenv("CF_INSTANCE_INDEX"),
                        System.getenv("CF_INSTANCE_GUID"),
                        System.getenv("CF_INSTANCE_IP"));

        // Activate socks proxy (if your application requires it)
        SocksProxyActivator.activate(new SocksProxyConfig("sitemap.properties", "sitemap.user.properties"));

        SpringApplication.run(SitemapApplication.class, args);
    }

}
