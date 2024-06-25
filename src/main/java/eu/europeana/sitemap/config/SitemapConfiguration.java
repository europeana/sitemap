package eu.europeana.sitemap.config;


import eu.europeana.features.S3ObjectStorageClient;
import eu.europeana.sitemap.exceptions.SiteMapConfigException;
import eu.europeana.sitemap.mongo.MongoProvider;
import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.net.MalformedURLException;
import java.net.URL;

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
    private URL entityApi;

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
            this.entityApi = new URL(entityApiUrl);
        } catch (MalformedURLException e) {
            throw new SiteMapConfigException("Property entity.api.url is incorrect: " + entityApiUrl, e);
        }

        // trim to avoid problems with accidental trailing spaces
        this.portalBaseUrl = this.portalBaseUrl.trim();
    }

    /**
     * Location where all sitemap files are stored
     * @return object storage client
     */
    @Bean
    public S3ObjectStorageClient objectStorageClient() {
        // for IBM Cloud S3 storage we need to provide an endpoint
        return new S3ObjectStorageClient(key, secret, region, bucket, endpoint);
    }

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
     * @param entityApi
     */
    public void setEntityApi(URL entityApi) {
        this.entityApi = entityApi;
        this.entityApiUrl = entityApi.toString();
    }

    public String getEntityApiKey() {
        return entityApiKey;
    }

    /**
     * Only used for testing purposes
     * @param entityApiKey
     */
    public void setEntityApiKey(String entityApiKey) {
        this.entityApiKey = entityApiKey;
    }

    public URL getEntityApi() {
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
