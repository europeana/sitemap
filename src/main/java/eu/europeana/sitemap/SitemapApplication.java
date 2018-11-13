package eu.europeana.sitemap;

import eu.europeana.features.ObjectStorageClient;
import eu.europeana.features.S3ObjectStorageClient;
import eu.europeana.sitemap.mongo.MongoProvider;
import eu.europeana.sitemap.service.ActiveSiteMapService;
import eu.europeana.sitemap.service.GenerateSitemapServiceImpl;
import eu.europeana.sitemap.service.ReadSitemapServiceImpl;
import eu.europeana.sitemap.service.ResubmitService;
import eu.europeana.sitemap.service.UpdateScheduler;
import eu.europeana.sitemap.web.context.SocksProxyConfigInjector;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Main application and configuration
 * @author Patrick Ehlert on 14-11-17.
 */
@SpringBootApplication
@EnableAutoConfiguration(exclude={MongoAutoConfiguration.class})
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

    @Value("${mongo.hosts}")
    private String hosts;
    @Value("${mongo.port}")
    private String port;
    @Value("${mongo.authDatabase}")
    private String authDatabase;
    @Value("${mongo.username}")
    private String username;
    @Value("${mongo.password}")
    private String password;
    @Value("${mongo.database}")
    private String database;

    /**
     * Mongo database from which we retrieve all records
     * @return
     */
    @Bean
    public MongoProvider mongoProvider() {
        return new MongoProvider(hosts, port, authDatabase, username, password, database);
    }

    /**
     * Location where all sitemap files are stored (Amazon S3)
     * @return
     */
    @Bean
    public ObjectStorageClient objectStorageClient() {
        return new S3ObjectStorageClient(key, secret, region, bucket);
    }

    /**
     * Regularly schedules an new sitemap update (if this is configured in sitemap.properties)
     * @return
     */
    @Bean
    public UpdateScheduler updateScheduler() {
        return new UpdateScheduler(generateSitemapService());
    }

    /**
     * Notifies Google and Bing after a sitemap update is completed
     * @return
     */
    @Bean
    public ResubmitService resubmitSitemapService() {
        return new ResubmitService();
    }

    /**
     * Determines which version of the sitemap files is active (green/blue deployment)
     * @return
     */
    @Bean
    public ActiveSiteMapService activeSitemapService() {
        return new ActiveSiteMapService(objectStorageClient());
    }

    /**
     * Main application service1 for reading files from s3
     * @return
     */
    @Bean
    public ReadSitemapServiceImpl readSitemapService() {
        return new ReadSitemapServiceImpl(objectStorageClient());
    }

    /**
     * Main application service2 that generates a new sitemap
     * @return
     */
    @Bean
    public GenerateSitemapServiceImpl generateSitemapService() {
        return new GenerateSitemapServiceImpl(mongoProvider(), objectStorageClient(), activeSitemapService(), readSitemapService(), resubmitSitemapService());
    }

    /**
     * This method is called when starting as a Spring-Boot application (e.g. from your IDE)
     * @param args
     */
    @SuppressWarnings("squid:S2095") // to avoid sonarqube false positive (see https://stackoverflow.com/a/37073154/741249)
    public static void main(String[] args)  {
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
     * @param servletContext
     * @throws ServletException
     */
    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
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
