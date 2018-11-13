package eu.europeana.sitemap.service;


import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import eu.europeana.features.ObjectStorageClient;
import eu.europeana.sitemap.FileNames;
import eu.europeana.sitemap.exceptions.SiteMapConfigException;
import eu.europeana.sitemap.mongo.MongoProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.Date;

/**
 * Created by ymamakis on 11/16/15.
 * Major refactoring by Patrick Ehlert on May 31, 2018
 */
//@Service
public class SitemapUpdateRecordService extends SitemapUpdateAbstractService {


    private static final Logger LOG = LogManager.getLogger(SitemapUpdateRecordService.class);

    private static final String FILE_NAME_BASE = FileNames.SITEMAP_RECORD_FILENAME_BASE;
    /** Used mongo fields **/
    private static final String ABOUT = "about";
    private static final String LASTUPDATED = "timestampUpdated";
    private static final String COMPLETENESS = "europeanaCompleteness";

    private static final String UPDATE_IN_PROGRESS = "In progress";
    private static final String UPDATE_FINISHED = "Finished";

    public static final int NUMBER_OF_ELEMENTS = 45_000;

    private final MongoProvider mongoProvider;
    private final ObjectStorageClient objectStorageProvider;
    private final ReadSitemapService readSitemapService;
    private final ResubmitService resubmitService;

    @Value("${portal.base.url}")
    private String portalBaseUrl;
    @Value("${record.portal.urlpath}")
    private String portalRecordUrlPath;
    @Value("${record.min.completeness}")
    private int minRecordCompleteness;

    public SitemapUpdateRecordService(MongoProvider mongoProvider, ObjectStorageClient objectStorageProvider,
                                      ReadSitemapService readSitemapService, ResubmitService resubmitService) {
        super(SitemapType.RECORD, readSitemapService, resubmitService);
        LOG.info("init");
        this.mongoProvider = mongoProvider;
        this.objectStorageProvider = objectStorageProvider;
        this.readSitemapService = readSitemapService;
        this.resubmitService = resubmitService;
    }

    @PostConstruct
    private void init() throws SiteMapConfigException {
        // check configuration for required properties
        if (StringUtils.isEmpty(portalBaseUrl)) {
            throw new SiteMapConfigException("Property portal.base.url is not set");
        }
        // trim to avoid problems with accidental trailing spaces
        portalBaseUrl = portalBaseUrl.trim();

        if (StringUtils.isEmpty(portalRecordUrlPath)) {
            throw new SiteMapConfigException("Property record.portal.urlpath is not set");
        }
        portalRecordUrlPath = portalRecordUrlPath.trim();
    }


    /**
     * Delete the old sitemap files, generate new ones and switch blue/green deployment
     */
    public void generate() {
        DBCursor cur = getRecordData();

        ActiveSitemapService activeSitemapService = new ActiveSitemapService(objectStorageProvider, FILE_NAME_BASE);
        SitemapGenerator generator = new SitemapGenerator(objectStorageProvider, activeSitemapService.getInactiveFile(),
                portalBaseUrl, FILE_NAME_BASE, NUMBER_OF_ELEMENTS);
        activeSitemapService.deleteInactiveFiles();

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
            generator.addItem(about, String.valueOf(completeness), dateUpdated);
        }
        cur.close();

        generator.finish();
        activeSitemapService.switchFile();
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
        DBCursor cursor = col.find(query, fields).batchSize(NUMBER_OF_ELEMENTS);
        LOG.info("Query finished. Retrieving records...");
        return cursor;
    }

    //private void sendUpdateFailedEmail(Exception e) {
//        SimpleMailMessage mailMessage = new SimpleMailMessage();
//        mailMessage.setTo();
   // }

}
