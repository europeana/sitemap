package eu.europeana.sitemap;

import eu.europeana.features.ObjectStorageClient;
import eu.europeana.features.S3ObjectStorageClient;
import eu.europeana.sitemap.mongo.MongoProvider;
import eu.europeana.sitemap.web.context.SocksProxyConfigInjector;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.PropertySource;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Main application and configuration
 * @author Patrick Ehlert on 14-11-17.
 */
@SpringBootApplication(exclude={MongoAutoConfiguration.class})
@PropertySource("classpath:sitemap.properties")
@PropertySource(value = "classpath:sitemap.user.properties", ignoreResourceNotFound = true)
public class SitemapApplication extends SpringBootServletInitializer {

    @Value("${s3.key}")
    private String key;
    @Value("${s3.secret}")
    private String secret;
    @Value("${s3.region}")
    private String region;
    @Value("${s3.bucket}")
    private String bucket;
    @Value("${s3.endpoint}")
    private String endpoint;


    @Value("${mongodb.connectionUrl}")
    private String mongoConnectionUrl;
    @Value("${mongodb.record.dbname}")
    private String mongoDatabase;

    /**
     * Mongo database from which we retrieve all record information
     * @return mongo provider bean
     */
    @Lazy
    @Bean
    public MongoProvider mongoProvider() {
        return new MongoProvider(mongoConnectionUrl, mongoDatabase);
    }

    /**
     * Location where all sitemap files are stored (Amazon S3)
     * @return object storage client
     */
    @Bean
    public ObjectStorageClient objectStorageClient() {
        if (StringUtils.isEmpty(endpoint)) {
            return new S3ObjectStorageClient(key, secret, region, bucket);
        }
        // for IBM Cloud S3 storage we need to provide an endpoint
        return new S3ObjectStorageClient(key, secret, region, bucket, endpoint);
    }

    /**
     * This method is called when starting as a Spring-Boot application (e.g. from your IDE)
     * @param args main application arguments
     */
    @SuppressWarnings("squid:S2095") // to avoid sonarqube false positive (see https://stackoverflow.com/a/37073154/741249)
    public static void main(String[] args)  {
        LogManager.getLogger(SitemapApplication.class).info("MAIN START");
        LogManager.getLogger(SitemapApplication.class).info("CF_INSTANCE_INDEX  = {}", System.getenv("CF_INSTANCE_INDEX"));
        LogManager.getLogger(SitemapApplication.class).info("CF_INSTANCE_IP  = {}", System.getenv("CF_INSTANCE_IP"));
        try {
            injectSocksProxySettings();
            SpringApplication.run(SitemapApplication.class, args);
        } catch (IOException e) {
            LogManager.getLogger(SitemapApplication.class).fatal("Error reading properties", e);
            System.exit(-1);
        }
    }

    /**
     * This method is called when starting a 'traditional' war deployment (e.g. in Docker of Cloud Foundry)
     * @param servletContext main application servlet context
     * @throws ServletException
     */
    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        LogManager.getLogger(SitemapApplication.class).info("CF_INSTANCE_INDEX  = {}", System.getenv("CF_INSTANCE_INDEX"));
        LogManager.getLogger(SitemapApplication.class).info("CF_INSTANCE_IP  = {}", System.getenv("CF_INSTANCE_IP"));
        try {
            injectSocksProxySettings();
            super.onStartup(servletContext);
        } catch (IOException e) {
            throw new ServletException("Error reading properties", e);
        }
    }

    private static void injectSocksProxySettings() throws IOException {
        SocksProxyConfigInjector socksConfig = new SocksProxyConfigInjector("sitemap.properties");
        try {
            socksConfig.addProperties("sitemap.user.properties");
        } catch (IOException e) {
            // user.properties may not be available so only show warning
            LogManager.getLogger(SitemapApplication.class).warn("Cannot read sitemap.user.properties file");
        }
        socksConfig.inject();
    }
    
}
