package eu.europeana.sitemap.web;


import eu.europeana.sitemap.service.SitemapUpdateAbstractService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Rest controller for sitemaps. This supports starting an update and showing the current sitemap index file
 * Created by ymamakis on 11/16/15.
 */
public class AbstractSitemapController {


    private static final Logger LOG = LogManager.getLogger(AbstractSitemapController.class);

    private final SitemapUpdateAbstractService updateService;

    public AbstractSitemapController(SitemapUpdateAbstractService updateService) {
        this.updateService = updateService;
    }





}
