package eu.europeana.sitemap.exceptions;

import eu.europeana.api.commons_sb3.error.EuropeanaGlobalExceptionHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Global exception handler that catches all errors and logs the interesting ones
 * @author Patrick Ehlert
 * Created on 21-02-2018
 */
@ControllerAdvice
public class GlobalExceptionHandler extends EuropeanaGlobalExceptionHandler {

    private static final Logger LOG = LogManager.getLogger(GlobalExceptionHandler.class);

    /**
     * Checks if we should log an error (with stacktrace and all) and rethrows it
     * @param e thrown exception
     * @throws SiteMapException we always rethrow the handled exception
     */
    @ExceptionHandler(SiteMapException.class)
    public void handleSiteMapException(SiteMapException e) throws SiteMapException {
        if (e.doLog()) {
            LOG.error("SitemapException: {}", e.getMessage(), e);
        }
        throw e;
    }
}
