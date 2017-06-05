package eu.europeana.sitemap.web;


import eu.europeana.sitemap.service.SitemapService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;


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
    public String update(HttpServletResponse response) {
        service.update();
        return service.getIndexFile();
    }

    /**
     * Lists all files stored in the used bucket/container
     * @param response
     * @return
     */
    @RequestMapping(value = "files", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String files(HttpServletResponse response) {
        response.setContentType("text/html");
        return service.getFiles();
    }

    /**
     * Returns the contents of the a particular file
     * @param fileName
     * @param response
     * @return
     */
    @RequestMapping(value = "file", method = RequestMethod.GET, produces = { MediaType.TEXT_XML_VALUE, MediaType.TEXT_HTML_VALUE } )
    public String file(
            @RequestParam(value = "name", required = true, defaultValue = "") String fileName, HttpServletResponse response) {
        String contents = service.getFile(fileName);
        if (contents.startsWith("<?xml")) {
            response.setContentType("text/xml");
        } else {
            response.setContentType("text/html");
        }
        return service.getFile(fileName);
    }

    /**
     * Returns the contents of the index file
     * @param response
     * @return
     */
    @RequestMapping(value = "index", method = RequestMethod.GET, produces = MediaType.TEXT_XML_VALUE)
    public String index(HttpServletResponse response) {
        response.setContentType("text/xml");
        return service.getIndexFile();
    }



}
