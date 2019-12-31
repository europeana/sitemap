package eu.europeana.sitemap.config;


import eu.europeana.features.ObjectStorageClient;
import eu.europeana.features.S3ObjectStorageClient;
import eu.europeana.sitemap.SitemapApplication;
import eu.europeana.sitemap.exceptions.SiteMapConfigException;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Class that contains all configuration settings
 */
@Configuration
@RefreshScope
@PropertySource("classpath:sitemap.properties")
@PropertySource(value = "classpath:sitemap.user.properties", ignoreResourceNotFound = true)
public class SitemapConfiguration {

    // TODO Think about where we create MailSender + MongoProvider

    @Value("${admin.apikey}")
    private String adminKey;

    @Value("${portal.base.url}")
    private String portalBaseUrl;

    @Value("${record.cron.update}")
    private String recordUpdateInterval;
    @Value("${record.resubmit}")
    private boolean recordResubmit;
    @Value("${record.min.completeness:0}")
    private int recordMinCompleteness;

    @Value("${mongodb.connectionUrl}")
    private String mongoConnectionUrl;
    @Value("${mongodb.record.dbname}")
    private String mongoDatabase;

    @Value("${entity.cron.update}")
    private String entityUpdateInterval;
    @Value("${entity.resubmit}")
    private boolean entityResubmit;
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

    @Value("${spring.mail.from:}")
    private String mailFrom;
    @Value("${spring.mail.to:}")
    private String mailTo;

    /**
     * Validate the most important settings
     * @throws SiteMapConfigException
     */
    protected SitemapConfiguration() throws SiteMapConfigException {
        LogManager.getLogger(SitemapConfiguration.class).error("Init sitemapconfig");
        if (StringUtils.isEmpty(portalBaseUrl)) {
            throw new SiteMapConfigException("Property portal.base.url is not set");
        }
        if (StringUtils.isEmpty(entityApiUrl)) {
            throw new SiteMapConfigException("Property entity.api.url is not set");
        }

        try {
            this.entityApi = new URL(entityApiUrl);
        } catch (MalformedURLException e) {
            throw new SiteMapConfigException("Property entity.api.url is incorrect: "+entityApiUrl);
        }

        // trim to avoid problems with accidental trailing spaces
        this.portalBaseUrl = this.portalBaseUrl.trim();
    }

    /**
     * Location where all sitemap files are stored (Amazon S3)
     * @return object storage client
     */
    @RefreshScope
    @Bean
    public ObjectStorageClient objectStorageClient() {
        LogManager.getLogger(SitemapApplication.class).info("Create objectstorage");
        if (StringUtils.isEmpty(endpoint)) {
            return new S3ObjectStorageClient(key, secret, region, bucket);
        }
        // for IBM Cloud S3 storage we need to provide an endpoint
        return new S3ObjectStorageClient(key, secret, region, bucket, endpoint);
    }


    public String getPortalBaseUrl() {
        return portalBaseUrl;
    }

    public String getRecordUpdateInterval() {
        return recordUpdateInterval;
    }

    public boolean isRecordResubmit() {
        return recordResubmit;
    }

    public int getRecordMinCompleteness() {
        return recordMinCompleteness;
    }

    public String getMongoConnectionUrl() {
        return mongoConnectionUrl;
    }

    public String getMongoDatabase() {
        return mongoDatabase;
    }

    public String getEntityUpdateInterval() {
        return entityUpdateInterval;
    }

    public boolean isEntityResubmit() {
        return entityResubmit;
    }

    public String getEntityApiUrl() {
        return entityApiUrl;
    }

    public String getEntityApiKey() {
        return entityApiKey;
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
