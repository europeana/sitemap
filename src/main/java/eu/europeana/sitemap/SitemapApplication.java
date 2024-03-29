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

/**
 * Main application and configuration
 *
 * @author Patrick Ehlert on 14-11-17.
 */
@SpringBootApplication(exclude = {MongoAutoConfiguration.class,
    MongoMetricsAutoConfiguration.class})
@PropertySource("classpath:build.properties")
public class SitemapApplication implements CommandLineRunner {

  private static final Logger logger = LogManager.getLogger(SitemapApplication.class);
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
      logger.info("No args provided to application. Starting web server");
      SpringApplication.run(SitemapApplication.class, args);
      return;
    }

    validateArg(args);
    // disable web server since we're only running an update task
    new SpringApplicationBuilder(SitemapApplication.class)
        .web(WebApplicationType.NONE)
        .run(args);
  }

  private static void validateArg(String[] args) {
    String taskArg = args[0];
    if (!RECORD.name().equalsIgnoreCase(taskArg)
        && !ENTITY.name().equalsIgnoreCase(taskArg)) {

      if (logger.isErrorEnabled()) {
        logger.error(
            "Unsupported argument '{}'. Supported arguments are '{}' and '{}'",
            taskArg,
            RECORD.name(),
            ENTITY.name());
      }
      System.exit(1);
    }
  }

  @Override
  public void run(String... args) throws Exception {
    if (args.length == 0) {
      // web server should be started
      return;
    }

    String taskArg = args[0];
    // arg already validated, so we know it's valid at this point
    UpdateAbstractService updateService =
        RECORD.name().equalsIgnoreCase(taskArg) ? updateRecordService : updateEntityService;
    logger.info("Starting automatic updating for {} sitemap...", updateService.getSitemapType());
    try {
      updateService.update();
    } catch (SiteMapException e) {
      logger.error("Error doing automatic update for {} sitemap", updateService.getSitemapType(),
          e);
    }
  }
}
