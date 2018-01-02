package eu.europeana.sitemap;

import eu.europeana.features.ObjectStorageClient;
import eu.europeana.features.S3ObjectStorageClient;
import eu.europeana.sitemap.mongo.MongoProvider;
import eu.europeana.sitemap.service.ActiveSiteMapService;
import eu.europeana.sitemap.service.GenerateSitemapServiceImpl;
import eu.europeana.sitemap.service.ReadSitemapServiceImpl;
import eu.europeana.sitemap.service.ResubmitService;
import eu.europeana.sitemap.service.UpdateScheduler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;

/**
 * Main application and configuration
 * @author Patrick Ehlert on 14-11-17.
 */
@SpringBootApplication
@EnableAutoConfiguration(exclude={MongoAutoConfiguration.class})
@PropertySource("classpath:sitemap.properties")
@PropertySource("classpath:sitemap.user.properties")
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
        return new MongoProvider(hosts, port, username, password, database);
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
     * Application start method
     * @param args
     */
    @SuppressWarnings("squid:S2095") // to avoid sonarqube false positive (see https://stackoverflow.com/a/37073154/741249)
    public static void main(String[] args) {
        SpringApplication.run(SitemapApplication.class, args);
    }
}
