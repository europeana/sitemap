package eu.europeana.sitemap.service;


import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import eu.europeana.sitemap.exceptions.SitemapNotReadyException;
import eu.europeana.sitemap.mongo.MongoProvider;
import eu.europeana.sitemap.swift.SwiftProvider;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.jclouds.io.Payload;
import org.jclouds.io.payloads.StringPayload;
import org.jclouds.openstack.swift.v1.domain.ObjectList;
import org.jclouds.openstack.swift.v1.domain.SwiftObject;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Date;
import java.util.logging.Logger;

/**
 * Created by ymamakis on 11/16/15.
 */
public class MongoSitemapService implements SitemapService {


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
    private static final int WEEKINSECONDS = 1000 * 60 * 60 * 24 * 7;
    private static final Logger log = Logger.getLogger(MongoSitemapService.class.getName());
    private static String status = "initial";
    public static final int NUMBER_OF_ELEMENTS = 45000;

    @Resource
    private MongoProvider mongoProvider;
    @Resource
    private SwiftProvider swiftProvider;
    @Resource
    private ActiveSiteMapService activeSiteMapService;

    public void generate() throws SitemapNotReadyException {
            DBCollection col = mongoProvider.getCollection();
            DBObject query = new BasicDBObject();

            DBObject fields = new BasicDBObject();
            fields.put("about", 1);
            fields.put("europeanaCompleteness", 1);
            fields.put("timestampUpdated", 1);
            DBCursor cur = col.find(query, fields).batchSize(NUMBER_OF_ELEMENTS);
            log.info("Got cursor");
            log.info("Cursor hasNext:" + cur.hasNext());
            int i = 0;
            StringBuilder master = new StringBuilder();
            master.append(XML_HEADER).append(LN);
            master.append(SITEMAP_HEADER).append(LN);

            StringBuilder slave = initializeSlaveGeneration();
            long startDate = new Date().getTime();
            while (cur.hasNext()) {

                DBObject obj = cur.next();
                String about = obj.get("about").toString();
                Date date = obj.get("timestampUpdated") != null ? (Date) obj.get("timestampUpdated") : new Date(0);
                String update = DateFormatUtils.format(date, DateFormatUtils.ISO_DATE_FORMAT.getPattern());
                int completeness = Integer.parseInt(obj.get("europeanaCompleteness").toString());
                String lastMod = "";
                if (date.getTime() > 0) {
                    lastMod = LASTMOD_OPENING + update + LASTMOD_CLOSING + LN;
                }
                slave.append(URL_OPENING).append(LN).append(LOC_OPENING).append(LN).append(PORTAL_URL)
                        .append(about).append(HTML).append(LN).append(LOC_CLOSING).append(PRIORITY_OPENING)
                        .append(completeness > 9 ? "1.0" : "0." + completeness)
                        .append(PRIORITY_CLOSING).append(lastMod).append(LN).append(URL_CLOSING).append(LN);
                if (i > 0 && (i % 45000 == 0 || !cur.hasNext())) {
                    String indexEntry = activeSiteMapService.getInactiveFile() + FROM + (i - NUMBER_OF_ELEMENTS) + TO + i;
                    master.append(SITEMAP_OPENING).append(LN).append(LOC_OPENING).append(StringEscapeUtils.escapeXml("http://www.europeana.eu/portal/" + indexEntry))
                            .append(LN).append(LOC_CLOSING).append(LN)
                            .append(SITEMAP_CLOSING).append(LN);
                    slave.append(URLSET_HEADER_CLOSING);
                    String fileName = activeSiteMapService.getInactiveFile() + FROM + (i - 45000) + TO + i;
                    saveToSwift(fileName, slave.toString());
                    slave = initializeSlaveGeneration();
                    long now = new Date().getTime();
                    log.info("Added " + i + " sitemap entries in " + (now - startDate) + " ms("+fileName+")");
                    startDate = now;
                }
                i++;
            }
            master.append(SITEMAP_HEADER_CLOSING);
            saveToSwift(MASTER_KEY, master.toString());
        log.info("Generation complete");
    }

    private boolean checkExists() {
        SwiftObject exists = swiftProvider.getObjectApi().getWithoutBody(MASTER_KEY);
        log.info("Exists: " + (exists != null));
        return exists != null;
    }

    private StringBuilder initializeSlaveGeneration() {
        return new StringBuilder().append(XML_HEADER).append(LN).append(URLSET_HEADER).append(LN);
    }


    private void saveToSwift(String key, String value) {
        Payload payload = new StringPayload(value);
        String ETag = swiftProvider.getObjectApi().put(key, payload);
        //Verify Data
        int nSaveAttempts = 1;
        boolean siteMapCacheFileExists = checkIfFileExists(key);
        if (StringUtils.isEmpty(ETag) || !siteMapCacheFileExists) {
            int MAX_ATTEMPTS = 3;
            while (nSaveAttempts < MAX_ATTEMPTS && (StringUtils.isEmpty(ETag) || !siteMapCacheFileExists)) {
                log.info("Failed to save to swift(Filename=" + key + ",siteMapCacheFileExists=" + siteMapCacheFileExists + ")");
                try {
                    long timeout = nSaveAttempts * 5000l;
                    log.info("Waiting " + timeout / 1000 + "seconds to try again");
                    Thread.sleep(timeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                log.info("Retrying to save the file");
                ETag = swiftProvider.getObjectApi().put(key, payload);
                siteMapCacheFileExists = checkIfFileExists(key);
                nSaveAttempts++;
            }
        }
    }

    private boolean checkIfFileExists(String key) {
        return swiftProvider.getObjectApi().getWithoutBody(key) != null;
    }

    public MongoProvider getMongoProvider() {
        return mongoProvider;
    }

    public void setMongoProvider(MongoProvider mongoProvider) {
        this.mongoProvider = mongoProvider;
    }

    public SwiftProvider getSwiftProvider() {
        return swiftProvider;
    }

    public void setSwiftProvider(SwiftProvider swiftProvider) {
        this.swiftProvider = swiftProvider;
    }

    public void delete() {
        ObjectList list = swiftProvider.getObjectApi().list();
        log.info("Files to remove: " + list.size());
        int i = 0;
        String inactiveFilename = activeSiteMapService.getInactiveFile();
        log.info("Deleting all old files with the name " + inactiveFilename);
        for (SwiftObject obj : list) {
            if (obj.getName().contains(inactiveFilename)) {
                swiftProvider.getObjectApi().delete(obj.getName());
                i++;
            }
            if (i == 100) {
                log.info("Removed 100 files");
            }
        }
        log.info("Removed all files");
    }

    @Override
    public void update() {
        log.info("Status :" + status);
        if (!(status.equalsIgnoreCase("working"))) {
            status = "working";
            delete();//First clear all old records from the inactive file
            generate();//Update records from the inactive file
            activeSiteMapService.switchFile();//Switch to updated cached files
            status = "done";
        } else {
            throw new SitemapNotReadyException();
        }

    }
}
