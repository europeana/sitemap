package eu.europeana.sitemap.service.update;


import com.mongodb.*;
import eu.europeana.sitemap.Constants;

import eu.europeana.features.ObjectStorageClient;
import eu.europeana.sitemap.config.PortalUrl;
import eu.europeana.sitemap.SitemapType;
import eu.europeana.sitemap.config.SitemapConfiguration;
import eu.europeana.sitemap.service.MailService;
import eu.europeana.sitemap.mongo.MongoProvider;
import eu.europeana.sitemap.service.ActiveDeploymentService;
import eu.europeana.sitemap.service.ReadSitemapService;
import eu.europeana.sitemap.service.ResubmitService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.Date;

/**
 * Service for updating the record sitemap. The primarily responsibility of this class is gathering record information
 * and adding each record to the sitemap. The rest of the update process is handled by the underlying abstract service.
 *
 * Created by ymamakis on 11/16/15.
 * Major refactoring by Patrick Ehlert on February 2019
 */
@Service
public class UpdateRecordService extends UpdateAbstractService {

    private static final Logger LOG = LogManager.getLogger(UpdateRecordService.class);

    private SitemapConfiguration config;
    private PortalUrl portalUrl;
    private MongoProvider mongoProvider;

    @Autowired
    public UpdateRecordService(ObjectStorageClient objectStorage, ActiveDeploymentService deploymentService,
                               ReadSitemapService readSitemapService, ResubmitService resubmitService, MailService mailService,
                               PortalUrl portalUrl, SitemapConfiguration config) {
        super(SitemapType.RECORD, objectStorage, deploymentService, readSitemapService, resubmitService, mailService, Constants.ITEMS_PER_SITEMAP_FILE);
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
        Cursor cursor = getRecordDataOnTiers();
        while (cursor.hasNext()) {
            DBObject obj = cursor.next();
            // gather the required data
            String about = obj.get(Constants.ABOUT).toString();
            int contentTier = Integer.parseInt(obj.get(Constants.CONTENT_TIER).toString());
            String metaDataTier = obj.get(Constants.METADATA_TIER).toString();
            Object timestampUpdated = obj.get(Constants.LASTUPDATED);
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
     * @see UpdateService#getUpdateInterval()
     */
    @Override
    public String getUpdateInterval() {
        return config.getRecordUpdateInterval();
    }

    /**
     * @see UpdateService#doResubmit()
     */
    @Override
    public boolean doResubmit() {
        return config.isRecordResubmit();
    }

    /**
     * Gets the record data based on contentTier, metadataTier value
     * Note: Don't pass value of the filter (in property file), we do not want to add
     * @return
     */
    private Cursor getRecordDataOnTiers() {
        DBCollection collection = mongoProvider.getCollection();
        AggregationOptions options = AggregationOptions.builder().batchSize(Constants.ITEMS_PER_SITEMAP_FILE).build();
        LOG.info("Starting record query...");
        Cursor  cursor = collection.aggregate(UpdateRecordServiceUtils.getPipeline(config.getRecordContentTier(), config.getRecordMetadataTier()), options);
        LOG.info("Query finished. Retrieving records...");
        return cursor;
    }

    @PreDestroy
    public void stopMongoConnections() {
        if (mongoProvider != null) {
            mongoProvider.close();
        }
    }

}
