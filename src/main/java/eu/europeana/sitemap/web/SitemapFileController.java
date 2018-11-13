package eu.europeana.sitemap.web;

import eu.europeana.sitemap.exceptions.SiteMapNotFoundException;
import eu.europeana.sitemap.service.ReadSitemapService;
import org.apache.logging.log4j.LogManager;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

/**
 * Generic functionality for reading sitemap files (for testing and debugging)
 *
 * @author Patrick Ehlert
 * Created on 30-05-2018
 */
@RestController
@RequestMapping("/")
public class SitemapFileController {

    protected final ReadSitemapService service;

    public SitemapFileController(ReadSitemapService service) {
        LogManager.getLogger(SitemapFileController.class).info("init debug READ controller");
        this.service = service;
    }

    /**
     * Lists all files stored in the used bucket/container (for debugging purposes)
     * @param response
     * @return
     */
    @RequestMapping(value = {"list", "files"}, method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    public String files(HttpServletResponse response) {
        return service.getFiles();
    }

    /**
     * Returns the contents of a particular file
     * @param fileName
     * @return
     */
    @RequestMapping(value = "file", method = RequestMethod.GET, produces = {MediaType.TEXT_PLAIN_VALUE, MediaType.TEXT_XML_VALUE})
    public String file(@RequestParam(value = "name", required = true, defaultValue = "") String fileName) throws SiteMapNotFoundException {
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("Please provide a file name");
        }
        return service.getFileContent(fileName);
    }
}
