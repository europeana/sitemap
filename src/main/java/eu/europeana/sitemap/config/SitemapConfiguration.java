package eu.europeana.sitemap.config;


import eu.europeana.s3.S3ObjectStorageClient;
import eu.europeana.sitemap.exceptions.SiteMapConfigException;
import eu.europeana.sitemap.mongo.MongoProvider;
import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Class that contains all configuration settings
 */
@Configuration
@PropertySource("classpath:sitemap.properties")
@PropertySource(value = "classpath:sitemap.user.properties", ignoreResourceNotFound = true)
public class SitemapConfiguration {

    @Value("${admin.apikey}")
    private String adminKey;

    @Value("${portal.base.url}")
    private String portalBaseUrl;

    @Value("${record.content.tier}")
    private String recordContentTier;
    @Value("${record.metadata.tier}")
    private String recordMetadataTier;
    @Value("${mongodb.connectionUrl}")
    private String mongoConnectionUrl;
    @Value("${mongodb.record.dbname}")
    private String mongoDatabase;

    @Value("${entity.api.url}")
    private String entityApiUrl;
    @Value("${entity.api.wskey}")
    private String entityApiKey;
    private URI entityApi;

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

    @Value("${spring.mail.from:#{null}}")
    private String mailFrom;
    @Value("${spring.mail.to:#{null}}")
    private String mailTo;

    protected SitemapConfiguration()  {
        LogManager.getLogger(SitemapConfiguration.class).debug("Init sitemap configuration");
    }


    @PostConstruct
    private void validateConfiguration() throws SiteMapConfigException {
        if (StringUtils.isEmpty(portalBaseUrl)) {
            throw new SiteMapConfigException("Property portal.base.url is not set");
        }
        if (StringUtils.isEmpty(entityApiUrl)) {
            throw new SiteMapConfigException("Property entity.api.url is not set");
        }

        try {
            this.entityApi = new URI(entityApiUrl);
        } catch (URISyntaxException e) {
            throw new SiteMapConfigException("Property entity.api.url is incorrect: " + entityApiUrl, e);
        }

        // trim to avoid problems with accidental trailing spaces
        this.portalBaseUrl = this.portalBaseUrl.trim();
    }

    /**
     * Location where all sitemap files are stored
     * @return object storage client
     * @throws URISyntaxException when the configured endpoint is not a valid URI
     */
    @Bean
    public S3ObjectStorageClient objectStorageClient() throws URISyntaxException {
        // for IBM Cloud S3 storage we need to provide an endpoint
        return new S3ObjectStorageClient(key, secret, region, bucket, new URI(endpoint));
    }

    /**
     * Create a new Mongo provider bean
     * @return MongoProvider bean
     */
    @Bean
    public MongoProvider mongoProvider() {
        return new MongoProvider(mongoConnectionUrl, mongoDatabase);
    }

    public String getPortalBaseUrl() {
        return portalBaseUrl;
    }

    public String getRecordContentTier() {
        return recordContentTier;
    }

    public String getRecordMetadataTier() {
        return recordMetadataTier;
    }

    public String getEntityApiUrl() {
        return entityApi.toString();
    }

    /**
     * Only used for testing purposes
     * @param entityApi URL to entity API endpoint
     */
    public void setEntityApi(URI entityApi) {
        this.entityApi = entityApi;
        this.entityApiUrl = entityApi.toString();
    }

    public String getEntityApiKey() {
        return entityApiKey;
    }

    /**
     * Only used for testing purposes
     * @param entityApiKey api key to use to connect to Entity API
     *
     */
    public void setEntityApiKey(String entityApiKey) {
        this.entityApiKey = entityApiKey;
    }

    public URI getEntityApi() {
        return entityApi;
    }

    public String getMailFrom() {
        return mailFrom;
    }

    public String getMailTo() {
        return mailTo;
    }

    public String getAdminKey() {
        return adminKey;
    }


}
