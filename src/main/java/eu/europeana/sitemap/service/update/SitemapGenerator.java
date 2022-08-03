package eu.europeana.sitemap.service.update;

import eu.europeana.features.ObjectStorageClient;
import eu.europeana.sitemap.config.PortalUrl;
import eu.europeana.sitemap.StorageFileName;
import eu.europeana.sitemap.service.Deployment;
import eu.europeana.sitemap.SitemapType;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jclouds.io.payloads.ByteArrayPayload;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Basic implementation of generating sitemap files and sitemap index file. Note that this class doesn't involve itself
 * in switching the active deployment, only in writing data to sitemap files.
 *
 * All generated sitemap file names start with the provided fileNameBase followed by a 'from' and 'to' parameter attached
 * to the name (the use of these parameters allows for easy processing of requested file names in a controller).
 *
 * When all items are added using the addItem() method you need to call finish() which will wrap up the generation process
 *
 * @author Patrick Ehlert
 * Created on 04-06-2018
 */
public class SitemapGenerator {

    private static final Logger LOG = LogManager.getLogger(SitemapGenerator.class);

    /** XML definitions **/
    private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    private static final String SITEMAP_HEADER_OPENING = "<sitemapindex xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">";
    private static final String SITEMAP_HEADER_CLOSING = "</sitemapindex>";

    private static final String SITEMAP_OPENING = "<sitemap>";
    private static final String SITEMAP_CLOSING = "</sitemap>";

    private static final String URLSET_HEADER = "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\"" +
                    " xmlns:image=\"http://www.google.com/schemas/sitemap-image/1.1\""+
                    " xmlns:geo=\"http://www.google.com/geo/schemas/sitemap/1.0\">";
    private static final String URLSET_HEADER_CLOSING = "</urlset>";

    private static final String URL_OPENING = "<url>";
    private static final String URL_CLOSING = "</url>";

    private static final String LOC_OPENING = "<loc>";
    private static final String LOC_CLOSING = "</loc>";

    private static final String PRIORITY_OPENING = "<priority>";
    private static final String PRIORITY_CLOSING = "</priority>";

    private static final String LASTMOD_OPENING = "<lastmod>";
    private static final String LASTMOD_CLOSING = "</lastmod>";

    private static final char LN = '\n';

    private static final String FROM_PARAM = "?from=";
    private static final String TO_PARAM = "&to=";

    private static final int MAX_SAVE_ATTEMPTS = 3;
    private static final long RETRY_SAVE_INTERVAL = 5000;
    private static final int MS_PER_SEC = 1000;

    private final ObjectStorageClient objectStorage;
    private final SitemapType type;

    private Deployment deployment;
    private String websiteBaseUrl;
    private int itemsPerSitemap;

    private boolean generationStarted;
    private boolean generationFinished;

    private StringBuilder sitemapIndex;
    private StringBuilder sitemap;
    private long fileStartTime; // this is for the current sitemap file

    // global stats
    private long nrRecords;
    private int nrSitemaps;
    private long from;

    /**
     * Setup a new sitemap generator
     * @param type sitemap type (record or entity)
     * @param objectStorage interface to S3 file storage
     */
    public SitemapGenerator(SitemapType type, ObjectStorageClient objectStorage) {
        this.objectStorage = objectStorage;
        this.type = type;
    }

    /**
     * Prepares the sitemap generation process.
     * @param desiredDeployment whether the saved files should be blue or green
     * @param websiteBaseUrl base url where sitemap files can be retrieved by search engines
     * @param itemsPerSitemap number of items per sitemap file
     */
    public void init(Deployment desiredDeployment, String websiteBaseUrl, int itemsPerSitemap) {
        if (generationStarted) {
            throw new IllegalStateException("Cannot start " + type + "sitemap generation. It's already started.");
        }
        LOG.info("Starting {} sitemap generation. Location of sitemap files {}", type, websiteBaseUrl);

        this.deployment = desiredDeployment;
        this.websiteBaseUrl = websiteBaseUrl;
        this.itemsPerSitemap = itemsPerSitemap;
        generationStarted = true;
        nrRecords = 0;
        nrSitemaps = 0;
        initSitemapIndex();
        initSitemapFile();
    }


    /**
     * Add an item/webpage to a sitemap file. Note that the first added item will count as the start of the generation process
     * @param url url of webpage that should be saved
     * @param priority priority of the webpage
     * @param dateLastModified last-modified date of the webpage
     */
    public void addItem(String url, String priority, Date dateLastModified) {
        if (generationFinished) {
            throw new IllegalStateException("Cannot add item; " + type + " sitemap generation is already finished.");
        }
        if (!generationStarted) {
            throw new IllegalStateException("Cannot add item; " + type + " sitemap generation is not started yet.");
        }
        appendItem(url, priority, dateLastModified);

        // check if this sitemap is full and we need to create a new one
        if (nrRecords % itemsPerSitemap == 0) {
            finishSitemapFile();
        }
    }

    /**
     * Write the current sitemap that's in progress as well as wrap up the index file. Note that this doesn't switch
     * from blue to green (or vice versa) deployment yet
     */
    public void finish() {
        if (!generationStarted) {
            throw new IllegalStateException("Cannot complete " + type + " sitemap generation. It hasn't started yet.");
        }
        if (generationFinished) {
            throw new IllegalStateException("Cannot complete " + type + " sitemap generation. It was already finished.");
        }
        finishSitemapFile();
        finishSitemapIndex();

        LOG.info("Items processed {}, written {} sitemap files and 1 sitemap index file", nrRecords, nrSitemaps);

        generationFinished = true;
    }

    private void initSitemapIndex() {
        LOG.debug("Starting new index...");
        this.sitemapIndex = new StringBuilder().append(XML_HEADER).append(LN).append(SITEMAP_HEADER_OPENING).append(LN);
    }

    private void finishSitemapIndex() {
        this.sitemapIndex.append(SITEMAP_HEADER_CLOSING);

        String fileName = StorageFileName.getSitemapIndexFileName(type, deployment);
        String fileContents = this.sitemapIndex.toString();
        LOG.debug("Generated contents for sitemap index\n{}", fileContents);
        if (saveToStorage(fileName, fileContents)) {
            LOG.info("Created sitemap file {}", fileName);
        }
    }

    private void initSitemapFile() {
        if (sitemap != null) {
            throw new IllegalStateException("Cannot start new sitemap file. Existing one isn't done yet");
        }
        LOG.debug("Starting new sitemap file...");
        this.fileStartTime = System.currentTimeMillis();
        this.from = nrRecords + 1;
        this.sitemap = new StringBuilder().append(XML_HEADER).append(LN).append(URLSET_HEADER).append(LN);
    }

    /**
     * Closes the current sitemap file, saves it to storage and adds the sitemap file to the sitemap index
     */
    private void finishSitemapFile() {
        if (sitemap == null) {
            throw new IllegalStateException("No sitemap file to finish!");
        }

        // add fileName to index (filename is location where file is retrievable for search engines)
        String fromToText = FROM_PARAM + from + TO_PARAM + nrRecords;
        String sitemapFileName = PortalUrl.getSitemapUrlEncoded(websiteBaseUrl, type, fromToText);
        LOG.debug("Add sitemap file {} to index", sitemapFileName);
        sitemapIndex.append(SITEMAP_OPENING).append(LN)
                .append(LOC_OPENING)
                .append(sitemapFileName)
                .append(LOC_CLOSING)
                .append(LN)
                // TODO if we can compare a sitemap file with the previous version, we can check if it has changed and include a lastmodified?
                //.append(generateLastModified(new Date()).toString())
                .append(SITEMAP_CLOSING)
                .append(LN);

        // write sitemap file, note that the actual filename in storage also contains blue-green information
        sitemap.append(URLSET_HEADER_CLOSING);
        String fileName = StorageFileName.getSitemapFileName(type, deployment, fromToText);
        String fileContents = sitemap.toString();
        LOG.debug("Generated contents for file {}\n{}", fileName, fileContents);
        nrSitemaps++;
        if (saveToStorage(fileName, fileContents)) {
            LOG.info("Created sitemap file {} in {} ms", fileName, (System.currentTimeMillis() - fileStartTime));
        }
        sitemap = null;
        initSitemapFile();
    }


    private void appendItem(String url, String priority, Date dateLastModified) {
        this.sitemap.append(URL_OPENING).append(LN)
                .append(LOC_OPENING)
                .append(url)
                .append(LOC_CLOSING)
                .append(LN);

        if (!StringUtils.isEmpty(priority)) {
            this.sitemap.append(PRIORITY_OPENING)
                    .append(priority)
                    .append(PRIORITY_CLOSING)
                    .append(LN);
        }

        if (dateLastModified != null) {
            this.sitemap.append(LASTMOD_OPENING)
                    .append(DateFormatUtils.format(dateLastModified, DateFormatUtils.ISO_DATE_FORMAT.getPattern()))
                    .append(LASTMOD_CLOSING)
                    .append(LN);
        }

        this.sitemap.append(URL_CLOSING).append(LN);

        nrRecords++;
    }

    private boolean saveToStorage(String key, String value) {
        boolean result = false;
        try (ByteArrayPayload payload = new ByteArrayPayload(value.getBytes(StandardCharsets.UTF_8))) {
            LOG.debug("Saving file with key {} and payload {}", key, payload);
            String eTag = objectStorage.put(key, payload);

            // verify is save was successful
            int nrSaveAttempts = 1;
            LOG.debug("Checking if file {} exists...", key);
            result = checkIfFileExists(key);
            while ((StringUtils.isEmpty(eTag) || !result) && (nrSaveAttempts < MAX_SAVE_ATTEMPTS)) {
                long timeout = nrSaveAttempts * RETRY_SAVE_INTERVAL;
                LOG.warn("Failed to save file {} to storage provider (etag = {}, siteMapCacheFileExists={}). "
                        +"Waiting {} seconds before trying again...", key, eTag, result, (timeout / MS_PER_SEC));
                Thread.sleep(timeout);

                LOG.info("Retry saving the file...");
                eTag = objectStorage.put(key, payload);
                result = checkIfFileExists(key);
                nrSaveAttempts++;
            }
            if (nrSaveAttempts >= MAX_SAVE_ATTEMPTS) {
                LOG.error("Failed to save file {} to storage provider. Giving up because we retried it {} times.", key, nrSaveAttempts);
            }
        } catch (InterruptedException e) {
            LOG.error("Saving to storage was interrupted", e);
            Thread.currentThread().interrupt();
        }
        return result;
    }

    private boolean checkIfFileExists(String id) {
        return objectStorage.isAvailable(id);
    }

}
