package eu.europeana.sitemap.service.update;


import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import eu.europeana.features.ObjectStorageClient;
import eu.europeana.sitemap.exceptions.SiteMapConfigException;
import eu.europeana.sitemap.mongo.MongoProvider;
import eu.europeana.sitemap.service.ActiveDeploymentService;
import eu.europeana.sitemap.service.ReadSitemapService;
import eu.europeana.sitemap.service.ResubmitService;
import eu.europeana.sitemap.SitemapType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
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

    private final MongoProvider mongoProvider;

    @Value("${portal.base.url}")
    private String portalBaseUrl;
    @Value("${record.portal.urlpath}")
    private String portalPath;
    @Value("${record.min.completeness:0}")
    private int minRecordCompleteness;
    @Value("${record.cron.update}")
    private String updateInterval;

    @Autowired
    public UpdateRecordService(MongoProvider mongoProvider, ObjectStorageClient objectStorage,
                               ActiveDeploymentService deploymentService, ReadSitemapService readSitemapService,
                               ResubmitService resubmitService) {
        super(SitemapType.RECORD, objectStorage, deploymentService, readSitemapService, resubmitService, ITEMS_PER_SITEMAP_FILE);
        this.mongoProvider = mongoProvider;
    }

    @PostConstruct
    private void checkConfiguration() throws SiteMapConfigException {
        if (StringUtils.isEmpty(portalBaseUrl)) {
            throw new SiteMapConfigException("Property portal.base.url is not set");
        }
        // trim to avoid problems with accidental trailing spaces
        portalBaseUrl = portalBaseUrl.trim();

        if (StringUtils.isEmpty(portalPath)) {
            throw new SiteMapConfigException("Property record.portal.urlpath is not set");
        }
        portalPath = portalPath.trim();
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

            // TODO check how to generate full record url
            LOG.debug("Adding record {}, completeness = {}, updated = {}", about, completeness, dateUpdated);
            sitemapGenerator.addItem(about, String.valueOf(completeness), dateUpdated);
        }
        cur.close();
    }

    @Override
    public String getWebsiteBaseUrl() {
        return portalBaseUrl + portalPath;
    }

    /**
     * @see UpdateService#getUpdateInterval()
     */
    @Override
    public String getUpdateInterval() {
        return updateInterval;
    }

    private DBCursor getRecordData() {
        DBCollection col = mongoProvider.getCollection();

        DBObject query = new BasicDBObject();
        // 2017-05-30 as part of ticket #624 we are filtering records based on completeness value.
        // This is an experiment to see if high-quality records improve the number of indexed records
        if (minRecordCompleteness >= 0) {
            LOG.info("Filtering records based on Europeana Completeness score of at least {}", minRecordCompleteness);
            query.put(COMPLETENESS, new BasicDBObject("$gte", minRecordCompleteness));
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

}
