package eu.europeana.sitemap.web;


import eu.europeana.sitemap.exceptions.SiteMapNotFoundException;
import eu.europeana.sitemap.service.SitemapService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
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


    private static final Logger LOG = LoggerFactory.getLogger(SitemapGenerationController.class);

    @Resource
    private SitemapService service;

    @Value("#{sitemapProperties['admin.apikey']}")
    private String adminKey;

    /**
     * Start the sitemap update process
     * @param wskey apikey that verify access to the update procedure
     * @param response
     * @return The index file in plain text
     */
    @RequestMapping(value = "update", method = RequestMethod.GET, produces = MediaType.TEXT_XML_VALUE)
    public String update(@RequestParam(value = "wskey", required = true) String wskey,
                         HttpServletResponse response) throws SiteMapNotFoundException, IOException {
        try {
            if (verifyKey(wskey)) {
                service.update();
                // we try to return the index file, but since updating takes a long time the browser may already have timed-out
                return service.getIndexFileContent();
            }
        } catch (SecurityException e) {
            LOG.error("SecurityException", e);
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        }

        return null;
    }

    /**
     * For now we do a very simple verification and check if the key matches the one set in the sitemap.properties file
     * If there is no admin key set, we do not allow any updates
     */
    private boolean verifyKey(String wskey) {
        if (StringUtils.isEmpty(adminKey)) {
            throw new SecurityException("No updates are allowed");
        } else if (!adminKey.equals(wskey)) {
            throw new SecurityException("Invalid key");
        }
        return true;
    }

}
