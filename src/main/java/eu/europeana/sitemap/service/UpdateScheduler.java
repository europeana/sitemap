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
import javax.annotation.Resource;
import java.util.TimeZone;

/**
 * Automatically starts the update process using a cron scheduler as specified in the configuration
 * @author Patrick Ehlert on 18-9-17.
 */
@Component
@EnableScheduling
public class UpdateScheduler {

    private static final Logger LOG = LogManager.getLogger(UpdateScheduler.class);

    @Resource
    private MongoSitemapService mongoSitemapService;

    @Value("#{sitemapProperties['scheduler.cron.update']}")
    private String updateCronConfig;

    private ThreadPoolTaskScheduler scheduler;

    /**
     * Initialize scheduler according to cron settings in properties file. If no configuration is found
     * then it won't be scheduled (no automatic updates)
     */
    @PostConstruct
    public void init() {
        if (updateCronConfig == null) {
            LOG.warn("No update cron settings specified!");
        } else {
            scheduler = new ThreadPoolTaskScheduler();
            scheduler.setPoolSize(1);
            scheduler.initialize();
            TimeZone timezone = TimeZone.getTimeZone("Europe/Amsterdam");
            LOG.info("Cron update schedule is: {} {}", updateCronConfig, timezone.getID());
            scheduler.schedule(new UpdateRunnable(), new CronTrigger(updateCronConfig, timezone));
        }
    }

    private class UpdateRunnable implements Runnable {
        @Override
        public void run() {
            LOG.info("Update scheduler: starting update...");
            try {
                mongoSitemapService.update();
            } catch (SiteMapException e) {
                LOG.error("Error running update process", e);
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
