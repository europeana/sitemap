package eu.europeana.sitemap.service;

import eu.europeana.sitemap.exceptions.SiteMapException;
import eu.europeana.sitemap.exceptions.SiteMapNotFoundException;
import eu.europeana.sitemap.exceptions.UpdateAlreadyInProgressException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;

/**
 * Abstract class with basic functionality for updating a sitemap. Updating includes deleting old blue/green
 * deployment files, generating new files, switching deployment and notifying search engines that there's a new sitemap
 * (the latter only if the index page has changed, so we are sure there are differences).
 */
public abstract class SitemapUpdateAbstractService implements SitemapUpdateService {

    private static final Logger LOG = LogManager.getLogger(SitemapUpdateAbstractService.class);

    private static final String UPDATE_IN_PROGRESS = "In progress";
    private static final String UPDATE_FINISHED = "Finished";

    private final SitemapType sitemapType;
    private final ReadSitemapService readSitemapService;
    private final ResubmitService resubmitService;
    private String updateStatus = "initial";
    private Date updateStartTime;

    public SitemapUpdateAbstractService(SitemapType type, ReadSitemapService readSitemapService, ResubmitService resubmitService) {
        LOG.info("init");
        this.sitemapType = type;
        this.readSitemapService = readSitemapService;
        this.resubmitService = resubmitService;
    }

    /**
     * Start the sitemap update process. This will delete any old sitemap at the inactive blue/green instance first,
     * then create a new sitemap and finally switching to the blue/green instances.
     */
    public void update() throws SiteMapException {
        setUpdateInProgress();
        try {
            long startTime = System.currentTimeMillis();

            String oldIndex = getCurrentIndex(sitemapType.getIndexFileName());
            this.generate();
            LOG.info("Sitemap generation completed in {} seconds", (System.currentTimeMillis() - startTime) / 1000);

            this.notifySearchEngines(oldIndex, getCurrentIndex(sitemapType.getIndexFileName()));
        } catch (Exception e) {
            LOG.error("Error updating sitemap {}", e.getMessage(), e);
            //   sendUpdateFailedEmail(e);
            throw new SiteMapException("Error updating sitemap", e);
        } finally {
            setUpdateDone();
        }
    }

    /**
     * Do the actual sitemap generation
     */
    protected abstract void generate();

    /**
     * checks if we can start the update, or if an update is already in progress
     * @throws UpdateAlreadyInProgressException
     */
    private void setUpdateInProgress() throws UpdateAlreadyInProgressException {
        // TODO instead of locking based on the status variable, it would be much better to lock based on a file placed in the storage provider.
        // This way we prevent multiple instances simultaneously updating records. We do however need a good mechanism to
        // clean any remaining lock from failed applications.
        synchronized(this) {
            if (UPDATE_IN_PROGRESS.equalsIgnoreCase(updateStatus)) {
                String msg = "There is already an update in progress (started at " + updateStartTime + ")";
                LOG.warn(msg);
                throw new UpdateAlreadyInProgressException(msg);
            } else {
                updateStatus = UPDATE_IN_PROGRESS;
                updateStartTime = new Date();
                LOG.info("Starting update process...");
            }
        }
    }

    private void setUpdateDone() {
        synchronized(this) {
            updateStartTime = null;
            updateStatus = UPDATE_FINISHED;
            LOG.info("Status: {}", updateStatus);
        }
    }

    /**
     * Temporarily save
     * @return
     */
    private String getCurrentIndex(String indexFileName) {
        String result = null;
        try {
            result = readSitemapService.getFileContent(indexFileName);
        } catch (SiteMapNotFoundException e) {
            LOG.warn("File {} not found", indexFileName);
        }
        return result;
    }

    /**
     * Notify search engines, but only if index file has changed
     */
    private void notifySearchEngines(String oldIndex, String newIndex) {
        if (newIndex == null) {
            LOG.error("Unable to determine if index has changed!");
        } else if (newIndex.equalsIgnoreCase(oldIndex)) {
            LOG.info("Index has not changed");
        } else {
            LOG.info("Index has changed");
            resubmitService.notifySearchEngines();
        }
    }



}
