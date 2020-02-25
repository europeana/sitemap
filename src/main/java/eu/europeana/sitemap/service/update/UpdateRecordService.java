package eu.europeana.sitemap.service.update;


import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
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

    /** Used mongo fields **/
    private static final String ABOUT = "about";
    private static final String LASTUPDATED = "timestampUpdated";
    private static final String COMPLETENESS = "europeanaCompleteness";
    private static final int ITEMS_PER_SITEMAP_FILE = 45_000;


    private SitemapConfiguration config;
    private PortalUrl portalUrl;
    private MongoProvider mongoProvider;

    @Autowired
    public UpdateRecordService(ObjectStorageClient objectStorage, ActiveDeploymentService deploymentService,
                               ReadSitemapService readSitemapService, ResubmitService resubmitService, MailService mailService,
                               PortalUrl portalUrl, SitemapConfiguration config) {
        super(SitemapType.RECORD, objectStorage, deploymentService, readSitemapService, resubmitService, mailService, ITEMS_PER_SITEMAP_FILE);
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
        DBCursor cur = getRecordData();
        while (cur.hasNext()) {
            // gather the required data
            DBObject record = cur.next();
            String about = record.get(ABOUT).toString();
            int completeness = Integer.parseInt(record.get(COMPLETENESS).toString());
            Object timestampUpdated = record.get(LASTUPDATED);
            // very old records do not have a timestampUpdated or timestampCreated field
            Date dateUpdated = (timestampUpdated == null ? null : (Date) timestampUpdated);

            String url = portalUrl.getRecordUrl(about);
            String completenessStr = (completeness > 9 ? "1.0" : ("0." + completeness));
            LOG.trace("Adding record {}, completeness = {}, updated = {}", url, completeness, dateUpdated);
            sitemapGenerator.addItem(url, completenessStr, dateUpdated);
        }
        cur.close();
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

    private DBCursor getRecordData() {
        DBCollection col = mongoProvider.getCollection();

        DBObject query = new BasicDBObject();
        // 2017-05-30 as part of ticket #624 we are filtering records based on completeness value.
        // This is an experiment to see if high-quality records improve the number of indexed records
        if (config.getRecordMinCompleteness() >= 0) {
            LOG.info("Filtering records based on Europeana Completeness score of at least {}", config.getRecordMinCompleteness());
            query.put(COMPLETENESS, new BasicDBObject("$gte", config.getRecordMinCompleteness()));
        }

        DBObject fields = new BasicDBObject();
        fields.put(ABOUT, 1);
        fields.put(COMPLETENESS, 1);
        fields.put(LASTUPDATED, 1);

        LOG.info("Starting record query...");
        DBCursor cursor = col.find(query, fields).batchSize(ITEMS_PER_SITEMAP_FILE);
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
