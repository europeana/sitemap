package eu.europeana.sitemap.service;

import eu.europeana.features.ObjectStorageClient;
import eu.europeana.sitemap.FileNames;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jclouds.io.payloads.ByteArrayPayload;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Generates sitemap files and a sitemap index files. All generated sitemap file names start with the provided fileNameBase
 * followed by a 'from' and 'to' parameter attached to the name (the use of these parameters allows for easy
 * processing of requested file names in a controller).
 *
 * When all items are added using the addItem() method you need to call finish() which will (over)write the index file
 * and switch the active blue/green deployment
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

    private static final String FROM = "?from=";
    private static final String TO = "&to=";

    private final ObjectStorageClient objectStorageProvider;
    private final BlueGreenDeployment blueGreen;
    private final String websiteBaseUrl;
    private final String fileNameBase;
    private final int itemsPerSitemap;

    private boolean generationStarted = false;
    private boolean generationFinished = false;

    private StringBuilder sitemapIndex;
    private StringBuilder sitemap;
    private long fileStartTime; // this is for the current sitemap file

    // global stats
    private long nrRecords = 0;
    private int nrSitemaps = 0;
    private long from = 0;

    /**
     * Start a new sitemap generation
     * @param objectStorageProvider
     * @param websiteBaseUrl base url of where the generated sitemap files can be retrieved
     * @param fileNameBase base name of the generated sitemap files
     * @param itemsPerSitemap, maximum number of items per sitemap file
     */
    public SitemapGenerator(ObjectStorageClient objectStorageProvider, BlueGreenDeployment blueGreen,
                            String websiteBaseUrl, String fileNameBase, int itemsPerSitemap) {
        this.objectStorageProvider = objectStorageProvider;
        this.blueGreen = blueGreen;
        this.websiteBaseUrl = websiteBaseUrl;
        this.fileNameBase = fileNameBase;
        this.itemsPerSitemap = itemsPerSitemap;
    }

    /**
     * Starts the sitemap generation process. This action will delete all files in the objectStorage that contain are
     * not part of the provided deployment (so blue files will be deleted for a green deployment and vice versa)
     */
    private void init() {
        LOG.debug("Starting sitemap generation");
        generationStarted = true;
        nrRecords = 0;
        nrSitemaps = 0;
        initSitemapIndex();
        initSitemapFile();
    }



    /**
     * Add an item to a sitemap file.
     * @param url
     * @param priority
     * @param dateLastModified
     */
    public void addItem(String url, String priority, Date dateLastModified) {
        if (generationFinished) {
            throw new IllegalStateException("Cannot add more items. Sitemap generation is already finished.");
        }
        if (!generationStarted) {
            init();
        }
        appendItem(url, priority, dateLastModified);

        // check if this sitemap is full and we need to create a new one
        if (nrRecords % itemsPerSitemap == 0) {
            finishSitemapFile();
        }
    }

    /**
     * Write the current sitemap that's in progress as well as wrap up the index file. Then it will switch the
     * blue/green deployment
     */
    public void finish() {
        if (!generationStarted) {
            throw new IllegalStateException("Cannot complete sitemap generation. It hasn't started yet.");
        }
        if (generationFinished) {
            throw new IllegalStateException("Cannot complete sitemap generation. It was already finished.");
        }
        finishSitemapFile();
        finishSitemapIndex();
        LOG.info("Records processed {}, written {} sitemap files and 1 sitemap index file", nrRecords, nrSitemaps);

        generationStarted = false;
        generationFinished = true;
    }

    private void initSitemapIndex() {
        LOG.debug("Starting new index...");
        this.sitemapIndex = new StringBuilder().append(XML_HEADER).append(LN).append(SITEMAP_HEADER_OPENING).append(LN);
    }

    private void finishSitemapIndex() {
        this.sitemapIndex.append(SITEMAP_HEADER_CLOSING);

        String fileName = FileNames.getSitemapIndexFileName(fileNameBase);
        String fileContents = this.sitemapIndex.toString();
        LOG.debug("Generated contents for sitemap index\n{}", fileContents);
        if (saveToStorage(fileName, fileContents)) {
            LOG.info("Created sitemap index file");
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
        String fromToText = FROM + from + TO + nrRecords;
        String sitemapFileName = FileNames.getSitemapFileNameInIndex(websiteBaseUrl, fileNameBase, fromToText);
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
        String fileName = FileNames.getSitemapFileNameStorage(fileNameBase, blueGreen, fromToText);
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
            String eTag = objectStorageProvider.put(key, payload);

            // verify is save was successful
            int nrSaveAttempts = 1;
            int maxAttempts = 3;
            LOG.debug("Checking if file {} exists...", key);
            result = checkIfFileExists(key);
            while ((StringUtils.isEmpty(eTag) || !result) && (nrSaveAttempts < maxAttempts)) {
                long timeout = nrSaveAttempts * 5000L;
                LOG.warn("Failed to save file {} to storage provider (etag = {}, siteMapCacheFileExists={}). "
                        +"Waiting {} seconds before trying again...", key, eTag, result, (timeout / 1000));
                Thread.currentThread().sleep(timeout);

                LOG.info("Retry saving the file...");
                eTag = objectStorageProvider.put(key, payload);
                result = checkIfFileExists(key);
                nrSaveAttempts++;
            }
            if (nrSaveAttempts >= maxAttempts) {
                LOG.error("Failed to save file {} to storage provider. Giving up because we retried it {} times.", key, nrSaveAttempts);
            }
        } catch (InterruptedException e) {
            LOG.error("Saving to storage was interrupted", e);
            Thread.currentThread().interrupt();
        }
        return result;
    }

    private boolean checkIfFileExists(String id) {
        return objectStorageProvider.isAvailable(id);
    }

}
