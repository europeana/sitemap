package eu.europeana.sitemap.service.update;

import eu.europeana.features.ObjectStorageClient;
import eu.europeana.sitemap.SitemapType;
import eu.europeana.sitemap.StorageFileName;
import eu.europeana.sitemap.exceptions.SiteMapException;
import eu.europeana.sitemap.exceptions.SiteMapNotFoundException;
import eu.europeana.sitemap.exceptions.UpdateAlreadyInProgressException;
import eu.europeana.sitemap.service.ActiveDeploymentService;
import eu.europeana.sitemap.service.Deployment;
import eu.europeana.sitemap.service.ReadSitemapService;
import eu.europeana.sitemap.service.ResubmitService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.Period;

import java.util.Date;

/**
 * Abstract class with basic functionality for updating a sitemap. This consists of several steps:
 * <ol>
 *     <li>Get the inactive deployment</li>
 *     <li>Delete all inactive sitemap files</li>
 *     <li>Generate new sitemap files (the actual data should be produced by an implementing class)</li>
 *     <li>Finish the generation (close and save open files)</li>
 *     <li>Switch the active deployment</li>
 *     <li>Notify search engines (if the index file has changed)</li>
 * </ol>
 */
public abstract class UpdateAbstractService implements UpdateService {

    private static final Logger LOG = LogManager.getLogger(UpdateAbstractService.class);

    private static final String UPDATE_IN_PROGRESS = "In progress";
    private static final String UPDATE_FINISHED = "Finished";

    private final SitemapType sitemapType;
    private final ObjectStorageClient objectStorage;
    private final ActiveDeploymentService deploymentService;
    private final ReadSitemapService readSitemapService;
    private final ResubmitService resubmitService;
    private final int itemsPerSitemap;

    private String updateStatus = "initial";
    private Date updateStartTime;

    public UpdateAbstractService(SitemapType type, ObjectStorageClient objectStorage, ActiveDeploymentService deploymentService,
                                 ReadSitemapService readSitemapService, ResubmitService resubmitService, int itemsPerSitemap) {
        this.sitemapType = type;
        this.objectStorage = objectStorage;
        this.deploymentService = deploymentService;
        this.readSitemapService = readSitemapService;
        this.resubmitService = resubmitService;
        this.itemsPerSitemap = itemsPerSitemap;
    }

    /**
     * Start the sitemap update process;
     */
    public void update() throws SiteMapException {
        setUpdateInProgress();
        try {
            // 1. Get inactive deployment
            Deployment inactive = deploymentService.getInactiveDeployment(sitemapType);
            LOG.info("Inactive deployment is " + inactive);

            // 2. Delete inactive files
            deploymentService.deleteInactiveFiles(sitemapType);

            // 3. Generate new files
            SitemapGenerator generator = new SitemapGenerator(sitemapType, objectStorage);
            generator.init(inactive, this.getWebsiteBaseUrl(), itemsPerSitemap);
            long generateStartTime = System.currentTimeMillis();
            this.generate(generator);

            // 4. Finish generation
            generator.finish();
            LOG.info("{} sitemap generation completed in {}", sitemapType,
                    getDurationText(System.currentTimeMillis() - generateStartTime));

            // 5. Switch deployment
            LOG.debug("Switching deployment...");
            Deployment newDeploy = deploymentService.switchDeployment(sitemapType);
            LOG.info("New deployment is now {}", newDeploy);

            // 6. Notify search engines (only if index changed)
            this.notifySearchEngines();

        } catch (Exception e) {
            LOG.error("Error updating sitemap {}", e.getMessage(), e);
            // TODO add email notification
            //   sendUpdateFailedEmail(e);
            throw new SiteMapException("Error updating " + sitemapType + " sitemap", e);
        } finally {
            setUpdateDone();
        }
    }

    /**
     * Do the actual sitemap generation.
     */
    protected abstract void generate(SitemapGenerator sitemapGenerator) throws SiteMapException;

    /**
     * @return the baseUrl where sitemap files can be retrieved by search engines (for saving this info in sitemap index)
     */
    protected abstract String getWebsiteBaseUrl();

    /**
     * @see UpdateService#getSitemapType()
     */
    @Override
    public SitemapType getSitemapType() {
        return this.sitemapType;
    }

    /**
     * checks if we can start the update, or if an update is already in progress
     * @throws UpdateAlreadyInProgressException when there is already an update in progress
     */
    private void setUpdateInProgress() throws UpdateAlreadyInProgressException {
        // TODO instead of locking based on the updateStatus variable, it would be much better to lock based on a file placed in the storage provider.
        // This way we prevent multiple instances simultaneously updating records. We do however need a good mechanism to
        // clean any remaining lock from failed applications.
        synchronized(this) {
            if (UPDATE_IN_PROGRESS.equalsIgnoreCase(updateStatus)) {
                String msg = "There is already an update in progress (started at " + updateStartTime + ")";
                LOG.warn(msg);
                throw new UpdateAlreadyInProgressException(msg);
            } else {
                updateStatus = UPDATE_IN_PROGRESS;
                LOG.info("Starting update process...");
            }
        }
    }

    private void setUpdateDone() {
        synchronized(this) {
            updateStartTime = null;
            updateStatus = UPDATE_FINISHED;
            LOG.info("Update {}", updateStatus);
        }
    }

    private void notifySearchEngines() {
        String indexBlue = null;
        String indexGreen = null;
        String blueFileName = StorageFileName.getSitemapIndexFileName(sitemapType, Deployment.BLUE);
        String greenFileName = StorageFileName.getSitemapIndexFileName(sitemapType, Deployment.GREEN);
        try {
            indexBlue = readSitemapService.getFileContent(blueFileName);
            indexGreen = readSitemapService.getFileContent(greenFileName);
        } catch (SiteMapNotFoundException e) {
            // Note that the first time we run the application there is no green index, so it will always give a warning then
            LOG.warn("Could not read file {}", greenFileName);
        }
        if (indexBlue != null && !indexBlue.equalsIgnoreCase(indexGreen)) {
            resubmitService.notifySearchEngines(sitemapType);
        }
    }

    /**
     * @param durationInMs duration in miliseconds
     * @return string containing duration in easy readable format
     */
    private String getDurationText(long durationInMs) {
        String result;
        Period period = new Period(durationInMs);
        if (period.getDays() >= 1) {
            result = String.format("%d days, %d hours and %d minutes", period.getDays(), period.getHours(), period.getMinutes());
        } else if (period.getHours() >= 1) {
            result = String.format("%d hours and %d minutes", period.getHours(), period.getMinutes());
        } else if (period.getMinutes() >= 1){
            result = String.format("%d minutes and %d seconds", period.getMinutes(), period.getSeconds());
        } else {
            result = String.format("%d.%d seconds", period.getSeconds(), period.getMillis());
        }
        return result;
    }

}
