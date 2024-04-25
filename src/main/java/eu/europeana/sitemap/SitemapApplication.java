package eu.europeana.sitemap;

import static eu.europeana.sitemap.SitemapType.ENTITY;
import static eu.europeana.sitemap.SitemapType.RECORD;

import eu.europeana.sitemap.exceptions.SiteMapException;
import eu.europeana.sitemap.service.update.UpdateAbstractService;
import eu.europeana.sitemap.service.update.UpdateEntityService;
import eu.europeana.sitemap.service.update.UpdateRecordService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.actuate.autoconfigure.metrics.mongo.MongoMetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.PropertySource;

import java.util.Arrays;

/**
 * Main application and configuration
 */
@SpringBootApplication(exclude = {MongoAutoConfiguration.class, MongoMetricsAutoConfiguration.class})
@PropertySource("classpath:build.properties")
public class SitemapApplication implements CommandLineRunner {

    private static final Logger LOG = LogManager.getLogger(SitemapApplication.class);
    @Autowired
    private UpdateEntityService updateEntityService;
    @Autowired
    private UpdateRecordService updateRecordService;

    /**
     * This method is called when starting as a Spring-Boot application (e.g. from your IDE)
     *
     * @param args main application arguments
     */
    @SuppressWarnings("squid:S2095")
    // to avoid sonarqube false positive (see https://stackoverflow.com/a/37073154/741249)
    public static void main(String[] args) {
        if (args.length == 0) {
            LOG.info("Starting web server");
            SpringApplication.run(SitemapApplication.class, args);
            return;
        }

        validateArg(args);
        // Start update, so disable web server
        new SpringApplicationBuilder(SitemapApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }

    private static void validateArg(String[] args) {
        if (args.length > 1) {
            LOG.error("Only 1 argument accepted!");
            System.exit(1);
        }
        String taskArg = args[0];
        if (!RECORD.name().equalsIgnoreCase(taskArg)  && !ENTITY.name().equalsIgnoreCase(taskArg)) {
            if (LOG.isErrorEnabled()) {
                LOG.error(
                        "Unsupported argument '{}'. Supported arguments are '{}' and '{}'",
                        taskArg,
                        RECORD.name(),
                        ENTITY.name());
            }
            System.exit(1);
        }
    }

    @Override
    public void run(String... args) {
        if (args.length > 0) {
            LOG.info("Command-line arguments = {}", Arrays.stream(args).toArray());
            String taskArg = args[0];
            // arg already validated, so we know it's valid at this point
            UpdateAbstractService updateService =
                    RECORD.name().equalsIgnoreCase(taskArg) ? updateRecordService : updateEntityService;
            LOG.info("Starting automatic updating for {} sitemap...", updateService.getSitemapType());
            try {
                updateService.update();
            } catch (SiteMapException e) {
                LOG.error("Error doing automatic update for {} sitemap", updateService.getSitemapType(), e);
            }
        } else {
            LOG.info("No command-line arguments provided");
        }
    }
}
