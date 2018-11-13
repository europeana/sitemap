package eu.europeana.sitemap.service;

import eu.europeana.sitemap.exceptions.SiteMapException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.TimeZone;

/**
 * Automatically starts the update process using a cron scheduler as specified in the configuration
 * @author Patrick Ehlert on 18-9-17.
 */
@Component
@EnableScheduling
public class UpdateScheduler {

    private static final Logger LOG = LogManager.getLogger(UpdateScheduler.class);

    private final SitemapUpdateAbstractService recordSitemapService;
    private final SitemapUpdateAbstractService entitySitemapService;

    @Value("${record.cron.update}")
    private String updateRecordConfig;

    @Value("${entity.cron.update}")
    private String updateEntityConfig;

    private ThreadPoolTaskScheduler scheduler;

    public UpdateScheduler(SitemapUpdateRecordService recordSitemapService,
                           SitemapUpdateEntityService entitySitemapService) {
        this.recordSitemapService = recordSitemapService;
        this.entitySitemapService = entitySitemapService;
    }

    /**
     * Initialize scheduler according to cron settings in properties file. If no configuration is found
     * then it won't be scheduled (no automatic updates)
     */
    @PostConstruct
    public void init() {
        if (updateRecordConfig == null) {
            LOG.warn("No cron settings specified for updating records!");
        }
        if (updateEntityConfig == null) {
            LOG.warn("No cron settings specified for updating entities!");
        }

        if (updateRecordConfig != null || updateEntityConfig != null) {
            scheduler = new ThreadPoolTaskScheduler();
            scheduler.setPoolSize(1);
            scheduler.initialize();
            TimeZone timezone = TimeZone.getTimeZone("Europe/Amsterdam");
            if (updateRecordConfig == null) {
                LOG.info("Record cron update schedule is: {} {}", updateRecordConfig, timezone.getID());
                scheduler.schedule(new UpdateRecordRunnable(), new CronTrigger(updateRecordConfig, timezone));
            }
            if (updateEntityConfig == null) {
                LOG.info("Entity cron update schedule is: {} {}", updateEntityConfig, timezone.getID());
                scheduler.schedule(new UpdateEntityRunnable(), new CronTrigger(updateEntityConfig, timezone));
            }
        }
    }

    private class UpdateRecordRunnable implements Runnable {
        @Override
        public void run() {
            LOG.info("Update record sitemap scheduler: starting update...");
            try {
                recordSitemapService.update();
            } catch (SiteMapException e) {
                LOG.error("Error running automatic update process for record sitemap: {}", e.getMessage(), e);
            }
        }
    }

    private class UpdateEntityRunnable implements Runnable {
        @Override
        public void run() {
            LOG.info("Update entity sitemap scheduler: starting update...");
            try {
                entitySitemapService.update();
            } catch (SiteMapException e) {
                LOG.error("Error running automatic update process for entity sitemap: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Clean up when the application is shutting down
     */
    @PreDestroy
    public void shutdown() {
        if (scheduler != null) {
            LOG.info("Shutting down scheduler...");
            scheduler.shutdown();
        }
    }

}
