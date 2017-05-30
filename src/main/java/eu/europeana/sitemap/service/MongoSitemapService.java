package eu.europeana.sitemap.service;


import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import eu.europeana.domain.StorageObject;
import eu.europeana.features.ObjectStorageClient;
import eu.europeana.sitemap.exceptions.SitemapNotReadyException;
import eu.europeana.sitemap.mongo.MongoProvider;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.jclouds.io.payloads.ByteArrayPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * Created by ymamakis on 11/16/15.
 */
public class MongoSitemapService implements SitemapService {


    private static final Logger LOG = LoggerFactory.getLogger(MongoSitemapService.class);

    private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    private static final String SITEMAP_HEADER =
            "<sitemapindex xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">";
    private static final String URLSET_HEADER =
            "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\" xmlns:image=\"http://www.google.com/schemas/sitemap-image/1.1\" xmlns:geo=\"http://www.google.com/geo/schemas/sitemap/1.0\">";
    private static final String URL_OPENING = "<url>";
    private static final String URL_CLOSING = "</url>";
    private static final String LOC_OPENING = "<loc>";
    private static final String LOC_CLOSING = "</loc>";
    private static final String LN = "\n";
    private static final String PORTAL_URL = "http://www.europeana.eu/portal/record";
    private static final String HTML = ".html";
    private static final String FROM = "?from=";
    private static final String TO = "&to=";
    private static final String SITEMAP_OPENING = "<sitemap>";
    private static final String SITEMAP_CLOSING = "</sitemap>";
    private static final String SITEMAP_HEADER_CLOSING = "</sitemapindex>";
    private static final String URLSET_HEADER_CLOSING = "</urlset>";
    private static final String PRIORITY_OPENING = "<priority>";
    private static final String PRIORITY_CLOSING = "</priority>";
    private static final String LASTMOD_OPENING = "<lastmod>";
    private static final String LASTMOD_CLOSING = "</lastmod>";
    private static final String MASTER_KEY = "europeana-sitemap-index-hashed.xml";
    private static final String SLAVE_KEY = "europeana-sitemap-hashed.xml";

    public static final int NUMBER_OF_ELEMENTS = 45000;

    @Resource
    private MongoProvider mongoProvider;
    @Resource
    private ObjectStorageClient objectStorageProvider;
    @Resource
    private ActiveSiteMapService activeSiteMapService;

    @Value("#{sitemapProperties['min.record.completeness']}")
    private int minRecordCompleteness;

    private String status = "initial";


    public void generate() throws SitemapNotReadyException {
        DBCollection col = mongoProvider.getCollection();

        DBObject query = new BasicDBObject();
        // 2017-05-30 as part of ticket #624 we are filtering records based on completeness value.
        // This is an experiment to see if high-quality records improve the number of indexed records
        LOG.info("Filtering records based on Europeana Completeness score of at least "+minRecordCompleteness);
        query.put("europeanaCompleteness", new BasicDBObject( "$gte", minRecordCompleteness));

        DBObject fields = new BasicDBObject();
        fields.put("about", 1);
        fields.put("europeanaCompleteness", 1);
        fields.put("timestampUpdated", 1);

        LOG.info("Starting record query...");
        DBCursor cur = col.find(query, fields).batchSize(NUMBER_OF_ELEMENTS);
        LOG.info("Query finished. Retrieving records...");

        long nrRecords = 0;
        int nrSitemaps = 0;

        // create sitemap index file header
        StringBuilder master = new StringBuilder();
        master.append(XML_HEADER).append(LN);
        master.append(SITEMAP_HEADER).append(LN);

        // create sitemap file header
        long fileStartTime = new Date().getTime();
        StringBuilder slave = initializeSlaveGeneration();

        while (cur.hasNext()) {

            DBObject obj = cur.next();
            String about = obj.get("about").toString();
            int completeness = Integer.parseInt(obj.get("europeanaCompleteness").toString());
            Object timestampUpdated = obj.get("timestampUpdated");
            // very old records do not have a timestampUpdated or timestampCreated field
            StringBuilder lastModified = new StringBuilder();
            if (timestampUpdated != null) {
                lastModified.append(LASTMOD_OPENING);
                lastModified.append(DateFormatUtils.format((Date) timestampUpdated, DateFormatUtils.ISO_DATE_FORMAT.getPattern()));
                lastModified.append(LASTMOD_CLOSING);
                lastModified.append(LN);
            }

            slave.append(URL_OPENING).append(LN).append(LOC_OPENING).append(LN).append(PORTAL_URL)
                    .append(about).append(HTML).append(LN).append(LOC_CLOSING).append(PRIORITY_OPENING)
                    .append(completeness > 9 ? "1.0" : "0." + completeness)
                    .append(PRIORITY_CLOSING).append(lastModified.toString()).append(LN).append(URL_CLOSING).append(LN);
            nrRecords++;

            // add sitemap closing tags
            if (nrRecords > 0 && (nrRecords % NUMBER_OF_ELEMENTS == 0 || !cur.hasNext())) {
                String indexEntry = SLAVE_KEY + FROM + (nrRecords - NUMBER_OF_ELEMENTS) + TO + nrRecords;
                master.append(SITEMAP_OPENING).append(LN).append(LOC_OPENING).append(StringEscapeUtils.escapeXml("http://www.europeana.eu/portal/" + indexEntry))
                        .append(LN).append(LOC_CLOSING).append(LN)
                        .append(SITEMAP_CLOSING).append(LN);
                slave.append(URLSET_HEADER_CLOSING);
                String fileName = activeSiteMapService.getInactiveFile() + FROM + (nrRecords - NUMBER_OF_ELEMENTS) + TO + nrRecords;
                saveToStorage(fileName, slave.toString());
                slave = initializeSlaveGeneration();
                nrSitemaps++;
                long now = new Date().getTime();
                LOG.info("Records processed = "+nrRecords+". Created sitemap file "+fileName+" in " + (now - fileStartTime) + " ms");
                fileStartTime = now;
            }
        }
        cur.close();
        master.append(SITEMAP_HEADER_CLOSING);
        saveToStorage(MASTER_KEY, master.toString());
        LOG.info("Records processed = "+nrRecords+". Writen " +nrSitemaps+ " sitemap files and 1 sitemap index file");
    }

    private StringBuilder initializeSlaveGeneration() {
        return new StringBuilder().append(XML_HEADER).append(LN).append(URLSET_HEADER).append(LN);
    }

    private void saveToStorage(String key, String value) {
        ByteArrayPayload payload = new ByteArrayPayload(value.getBytes());
        String ETag = objectStorageProvider.put(key, payload);
        //Verify Data
        int nSaveAttempts = 1;
        boolean siteMapCacheFileExists = checkIfFileExists(key);
        if (StringUtils.isEmpty(ETag) || !siteMapCacheFileExists) {
            int MAX_ATTEMPTS = 3;
            while (nSaveAttempts < MAX_ATTEMPTS && (StringUtils.isEmpty(ETag) || !siteMapCacheFileExists)) {
                LOG.info("Failed to save to storage provider (Filename=" + key + ",siteMapCacheFileExists=" + siteMapCacheFileExists + ")");
                try {
                    long timeout = nSaveAttempts * 5000l;
                    LOG.info("Waiting " + timeout / 1000 + "seconds to try again");
                    Thread.sleep(timeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                LOG.info("Retrying to save the file");
                ETag = objectStorageProvider.put(key, payload);
                siteMapCacheFileExists = checkIfFileExists(key);
                nSaveAttempts++;
            }
        }
    }

    private boolean checkIfFileExists(String key) {
        return objectStorageProvider.getWithoutBody(key).isPresent();
    }

    public MongoProvider getMongoProvider() {
        return mongoProvider;
    }

    public void setMongoProvider(MongoProvider mongoProvider) {
        this.mongoProvider = mongoProvider;
    }

    public ObjectStorageClient getObjectStorageProvider() {
        return objectStorageProvider;
    }

    public void setObjectStorageProvider(ObjectStorageClient objectStorageProvider) {
        LOG.info("Object storage provider is "+objectStorageProvider.getName()+", bucket "+objectStorageProvider.getBucketName());
        this.objectStorageProvider = objectStorageProvider;
    }

    /**
     * Delete the old sitemap at the currently inactive blue/green instance
     */
    public void delete() {
        List<StorageObject> list = objectStorageProvider.list();
        if(list.isEmpty()){
            LOG.info("No files to remove.");
        }

        int i = 0;
        String inactiveFilename = activeSiteMapService.getInactiveFile();
        LOG.info("Deleting all old files with the name " + inactiveFilename);
        for (StorageObject obj : list) {
            if (obj.getName().contains(inactiveFilename)) {
                objectStorageProvider.delete(obj.getName());
                i++;
            }
            // report on progress
            if (i > 0 && i % 100 == 0) {
                LOG.info("Removed "+i+" files");
            }
        }
        LOG.info("Removed all "+i+" old files");
    }

    /**
     * @see SitemapService#update()
     */
    @Override
    public void update() {
        LOG.info("Status: " + status);
        if ("working".equalsIgnoreCase(status)) {
            throw new SitemapNotReadyException();
        } else {
            status = "working";
            LOG.info("Starting update process...");

            // First clear all old records from the inactive file
            delete();

            // Then write records to the inactive file
            long startTime = new Date().getTime();
            generate();
            LOG.info("Sitemap generation completed in "+ (new Date().getTime() - startTime) / 1000 +" seconds");

            String activeFile = activeSiteMapService.switchFile(); //Switch to updated cached files
            LOG.info("Switched active sitemap to "+activeFile);

            status = "done";
        }

    }
}
