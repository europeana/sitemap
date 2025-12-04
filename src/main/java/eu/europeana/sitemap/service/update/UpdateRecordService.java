package eu.europeana.sitemap.service.update;


import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import eu.europeana.s3.S3ObjectStorageClient;
import eu.europeana.sitemap.Constants;
import eu.europeana.sitemap.SitemapType;
import eu.europeana.sitemap.config.PortalUrl;
import eu.europeana.sitemap.config.SitemapConfiguration;
import eu.europeana.sitemap.mongo.MongoProvider;
import eu.europeana.sitemap.service.ActiveDeploymentService;
import jakarta.annotation.PreDestroy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * Service for updating the record sitemap. The primary responsibility of this class is gathering record information
 * and adding each record to the sitemap. The rest of the update process is handled by the underlying abstract service.
 *
 * Created by ymamakis on 11/16/15.
 * Major refactoring by Patrick Ehlert on February 2019 and June 2024
 */
@Service
public class UpdateRecordService extends AbstractUpdateService {

    private static final Logger LOG = LogManager.getLogger(UpdateRecordService.class);

    private final SitemapConfiguration config;
    private final PortalUrl portalUrl;
    private final MongoProvider mongoProvider;

    /**
     * Initialize the service to update the record sitemap
     * @param objectStorage the S3 object storage to write files to
     * @param deploymentService the deployment service
     * @param mailService the email service
     * @param portalUrl what url is used by Portal (the website)
     * @param config the application's configuration
     */
    @Autowired
    public UpdateRecordService(S3ObjectStorageClient objectStorage, ActiveDeploymentService deploymentService,
                               MailService mailService, PortalUrl portalUrl, SitemapConfiguration config) {
        super(SitemapType.RECORD, objectStorage, deploymentService, mailService, Constants.ITEMS_PER_SITEMAP_FILE);
        this.config = config;
        this.portalUrl = portalUrl;
        this.mongoProvider = config.mongoProvider();
    }

    /**
     * Generate record data (and save it with sitemapGenerator.addItem() method)
     * Never call this manually! It is automatically called by the UpdateAbstractService
     */
    @Override
    protected void generate(SitemapGenerator sitemapGenerator) {
        MongoCursor<Document> cursor = getRecordData();
        while (cursor.hasNext()) {
            Document doc = cursor.next();
            // gather the required data
            String about = doc.get(Constants.ABOUT).toString();
            int contentTier = Integer.parseInt(doc.get(Constants.CONTENT_TIER).toString());
            String metaDataTier = doc.get(Constants.METADATA_TIER).toString();
            Object timestampUpdated = doc.get(Constants.LASTUPDATED);
            // very old records do not have a timestampUpdated or timestampCreated field
            Date dateUpdated = (timestampUpdated == null ? null : (Date) timestampUpdated);

            String url = portalUrl.getRecordUrl(about);
            LOG.trace("Adding record {}, contentTier = {}, metadataTier = {} , updated = {}", url, contentTier, metaDataTier, dateUpdated);
            sitemapGenerator.addItem(url, UpdateRecordServiceUtils.getPriorityForTiers(contentTier), dateUpdated);
        }
        cursor.close();
    }

    @Override
    public String getWebsiteBaseUrl() {
        return config.getPortalBaseUrl();
    }

    /**
     * Gets the record data based on contentTier, metadataTier value
     * Note: Don't pass value of the filter (in property file), we do not want to add
     */
    private MongoCursor<Document> getRecordData() {
        MongoCollection<Document> collection = mongoProvider.getCollection();
        LOG.info("Starting record query...");
        MongoCursor<Document> cursor = collection
                .aggregate(UpdateRecordServiceUtils.getPipeline(config.getRecordContentTier(), config.getRecordMetadataTier()))
                .batchSize(Constants.ITEMS_PER_SITEMAP_FILE)
                .cursor();
        LOG.info("Query finished. Retrieving records...");
        return cursor;
    }

    /**
     * Close all connections to mongo
     */
    @PreDestroy
    public void stopMongoConnections() {
        if (mongoProvider != null) {
            mongoProvider.close();
        }
    }

}
