/*
 * Copyright 2007-2015 The Europeana Foundation
 *
 * Licenced under the EUPL, Version 1.1 (the "Licence") and subsequent versions as approved
 * by the European Commission;
 * You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 * http://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the Licence is distributed on an "AS IS" basis, without warranties or conditions of
 * any kind, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under
 * the Licence.
 */

package eu.europeana.sitemap.web;

import eu.europeana.sitemap.exceptions.SiteMapNotFoundException;
import eu.europeana.sitemap.service.ReadSitemapService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Handles requests for sitemap files from external parties
 * The controller checks which deployment is currently active (blue or green) and retrieves the correct file.
 * Note that there is only a blue/green version for sitemap files and not for the sitemap index file.
 * @author luthien, created on 07/12/2015.
 * @author Patrick Ehlert, major refactoring on 21/08/2017
 */
@RestController
@RequestMapping("/")
public class SitemapReadController {

    public static final String INDEX_FILE = "europeana-sitemap-index-hashed.xml";
    public static final String ACTIVE_SITEMAP_FILE = "europeana-sitemap-active-xml-file.txt";

    private static final Logger LOG = LogManager.getLogger(SitemapReadController.class);

    private final ReadSitemapService service;

    public SitemapReadController(ReadSitemapService service) {
        this.service = service;
    }

    /**
     * Return the sitemap index file
     *
     * @throws SiteMapNotFoundException if the index file wasn't found
     * @return contents of sitemap index file
     */
    @RequestMapping(value = {"index", "europeana-sitemap-index-hashed.xml"}, method = RequestMethod.GET)
    public String handleSitemapIndex() throws SiteMapNotFoundException {
        return service.getFileContent(INDEX_FILE);
    }

    /**
     * Return a sitemap file. Note that the to and from are fixed values, a list of all files with to/from values
     * can be found in the sitemap index
     *
     * @param from     start index
     * @param to       end index
     * @throws SiteMapNotFoundException if the sitemap file wasn't found
     * @return contents of sitemap file
     */
    @RequestMapping(value = "europeana-sitemap-hashed.xml", method = RequestMethod.GET)
    public String handleSitemapFile(@RequestParam(value = "from", required = true) String from,
                                    @RequestParam(value = "to", required = true) String to) throws SiteMapNotFoundException {
        String fileName = getActiveDeployment() + "?from=" + from + "&to=" + to;
        if (LOG.isDebugEnabled()) {
            LOG.debug("Retrieving sitemap file {} ", fileName);
        }
        return service.getFileContent(fileName);
    }

    /**
     * The active sitemap file stores either the value 'blue' or 'green' so we know which deployment of the files we
     * need to retrieve
     * @return
     * @throws SiteMapNotFoundException if the active deployment file was not found
     */
    private String getActiveDeployment() throws SiteMapNotFoundException {
        return service.getFileContent(ACTIVE_SITEMAP_FILE);
    }

    /**
     * Lists all files stored in the used bucket/container (only for debugging purposes)
     * @param response
     * @return
     */
    @RequestMapping(value = {"list", "files"}, method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    public String files(HttpServletResponse response) {
        return service.getFiles();
    }

    /**
     * Returns the contents of a particular file (only for debugging purposes)
     * @param fileName
     * @param response
     * @return
     */
    @RequestMapping(value = "file", method = RequestMethod.GET, produces = {MediaType.TEXT_PLAIN_VALUE, MediaType.TEXT_XML_VALUE})
    public String file(@RequestParam(value = "name", required = true, defaultValue = "") String fileName,
                       HttpServletResponse response) throws SiteMapNotFoundException {
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("Please provide a file name");
        }
        String contents = service.getFileContent(fileName);
        // TODO setting response content type to xml doesn't work. Response always has the (first-listed) produces = type)
        if (contents.startsWith("<?xml")) {
            response.setContentType("text/xml");
        } else {
            response.setContentType("text/plain");
        }
        return contents;
    }


}
