package eu.europeana.sitemap.service.update;

import eu.europeana.sitemap.exceptions.SiteMapException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.TimeZone;

/**
 * Automatically starts the update process for all sitemap types using a cron scheduler. Note that updates will
 * only be activated when deployed to a Cloud Foundry environment and then only on instance 0
 *
 * @author Patrick Ehlert on 18-9-17.
 */
@Service
@EnableScheduling
@Import({UpdateRecordService.class, UpdateEntityService.class})
public class UpdateScheduler {

    private static final Logger LOG = LogManager.getLogger(UpdateScheduler.class);

    private final List<UpdateService> updateServices;

    private ThreadPoolTaskScheduler scheduler;

    @Autowired
    public UpdateScheduler(List<UpdateService> updateServices) {
        this.updateServices = updateServices;
    }

    /**
     * Initialize scheduler according to specified cron settings. If no configuration is found then it won't be
     * scheduled (no automatic updates)
     */
    @PostConstruct
    public void init() {
        for (UpdateService updateService : updateServices) {
            if (StringUtils.isEmpty(updateService.getUpdateInterval())) {
                LOG.warn("No cron settings specified for updating {} sitemaps!", updateService.getSitemapType());
            } else {
                if (scheduler == null) {
                    scheduler = new ThreadPoolTaskScheduler();
                    scheduler.setPoolSize(1);
                    scheduler.initialize();
                }
                TimeZone timezone = TimeZone.getTimeZone("Europe/Amsterdam");
                LOG.info("{} sitemap update schedule is {} {}", updateService.getSitemapType(),
                        updateService.getUpdateInterval(), timezone.getID());
                scheduler.schedule(new UpdateRunnable(updateService),
                        new CronTrigger(updateService.getUpdateInterval(), timezone));
            }
        }
    }

    private class UpdateRunnable implements Runnable {

        private UpdateService updateService;

        UpdateRunnable(UpdateService updateService) {
            this.updateService = updateService;
        }

        @Override
        public void run() {
            // When deployed to Cloud Foundry there can be multiple instances, so we check the instance number and only
            // allow instance 0 to do updates!
            String instanceNr = System.getenv("CF_INSTANCE_INDEX");
            if ("0".equals(instanceNr)) {
                LOG.info("Starting automatic updating for {} sitemap...", updateService.getSitemapType());
                try {
                    updateService.update();
                } catch (SiteMapException e) {
                    LOG.error("Error doing automatic update for {} sitemap", updateService.getSitemapType(), e);
                }
            } else {
                LOG.info("Skipping automatic update because instance is {}", instanceNr);
            }
        }
    }

    /**
     * Clean up when the application is shutting down
     */
    @PreDestroy
    public void shutdown() {
        if (scheduler != null) {
            LOG.info("Shutting down update scheduler...");
            scheduler.shutdown();
        }
    }

}
