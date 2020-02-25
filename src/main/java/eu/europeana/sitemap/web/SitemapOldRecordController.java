package eu.europeana.sitemap.web;

import eu.europeana.sitemap.SitemapType;
import eu.europeana.sitemap.exceptions.SiteMapNotFoundException;
import eu.europeana.sitemap.service.ActiveDeploymentService;
import org.apache.logging.log4j.LogManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles requests for record sitemap files from external parties
 * The controller checks which deployment is currently active (blue or green) and retrieves the correct file.
 *
 * @author luthien, created on 07/12/2015.
 * @author Patrick Ehlert, major refactoring on 21/08/2017 and 30/05/2018
 * @deprecated will be removed shortly after the new record and entities urls are in production
 */
@RestController
@Deprecated
@RequestMapping("/sitemap")
public class SitemapOldRecordController extends SitemapAbstractController {

    public SitemapOldRecordController(ActiveDeploymentService activeDeployment, SitemapFileController readController) {
        super(SitemapType.RECORD, activeDeployment, readController);
    }

    /**
     * Old style record sitemap index retrieval
     */
    @Deprecated
    @GetMapping(value = "europeana-sitemap-index-hashed.xml")
    public String getOldRecordSitemapIndex() throws SiteMapNotFoundException {
        return super.getSitemapIndex();
    }

    /**
     * Old style record sitemap file retrieval
     */
    @Deprecated
    @GetMapping(value = "europeana-sitemap-hashed.xml")
    public String getOldRecordSitemapFile(@RequestParam(value = "from") String from,
                                          @RequestParam(value = "to") String to) throws SiteMapNotFoundException {
        // the from values have changed, instead of starting at 0 we now start at 1
        try {
            Integer fromInt = Integer.parseInt(from) + 1;
            return super.getSitemapFile(fromInt.toString(), to);
        } catch (NumberFormatException e) {
            LogManager.getLogger(SitemapOldRecordController.class).error("Error parsing 'from' value! ["+from+"]", e);
        }
        return super.getSitemapFile(from, to);
    }

}
