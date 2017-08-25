package eu.europeana.sitemap.web;


import eu.europeana.sitemap.exceptions.SiteMapNotFoundException;
import eu.europeana.sitemap.service.SitemapService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


/**
 * Rest controller for sitemaps. This supports starting an update and showing the current sitemap index file
 * Created by ymamakis on 11/16/15.
 */
@RestController
@RequestMapping("/")
public class SitemapGenerationController {

    @Resource
    private SitemapService service;

    /**
     * Start the sitemap update process
     * @param response
     * @return The index file in plain text
     */
    @RequestMapping(value = "update", method = RequestMethod.GET, produces = MediaType.TEXT_XML_VALUE)
    public String update(HttpServletResponse response) throws SiteMapNotFoundException, IOException {
        service.update();
        // we try to return the index file, but since updating takes a long time the browser may already have timed-out
        return service.getIndexFileContent();
    }

}
