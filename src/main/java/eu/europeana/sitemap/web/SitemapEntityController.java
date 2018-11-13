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

import eu.europeana.sitemap.FileNames;
import eu.europeana.sitemap.exceptions.SiteMapException;
import eu.europeana.sitemap.exceptions.SiteMapNotFoundException;
import eu.europeana.sitemap.service.SitemapUpdateEntityService;
import eu.europeana.sitemap.service.SitemapUpdateService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

/**
 * Handles requests for entity sitemap files from external parties
 * The controller checks which deployment is currently active (blue or green) and retrieves the correct file.
 * Note that there is only a blue/green version for sitemap files, but not for the sitemap index file.
 *
 * @author Patrick Ehlert
 * Created on 30-05-2018
 */
@RestController
@RequestMapping("/entity")
public class SitemapEntityController {

    private static final Logger LOG = LogManager.getLogger(SitemapEntityController.class);

    private SitemapFileController readController;
    private SitemapUpdateService updateService;

    @Value("${admin.apikey}")
    private String adminKey;

    public SitemapEntityController(SitemapFileController readController, SitemapUpdateEntityService updateService) {
        this.readController = readController;
        this.updateService = updateService;
    }

    /**
     * Return the entity sitemap index file
     *
     * @throws SiteMapNotFoundException if the index file wasn't found
     * @return contents of entity sitemap index file
     */
    @RequestMapping(value = {"index"}, method = RequestMethod.GET)
    public String handleEntitySitemapIndex() throws SiteMapNotFoundException {
        return readController.file(FileNames.SITEMAP_ENTITY_INDEX_FILE);
    }

    /**
     * Return an entity sitemap file. Note that the to and from are fixed values, a list of all files with to/from values
     * can be found in the sitemap index file
     *
     * @param from     start index
     * @param to       end index
     * @throws SiteMapNotFoundException if the sitemap file wasn't found
     * @return contents of sitemap file
     */
    @Deprecated
    @RequestMapping(value = FileNames.SITEMAP_ENTITY_FILENAME_BASE, method = RequestMethod.GET)
    public String handleEntitySitemapFile(@RequestParam(value = "from", required = true) String from,
                                          @RequestParam(value = "to", required = true) String to) throws SiteMapNotFoundException {
        String fileName = FileNames.SITEMAP_ENTITY_FILENAME_BASE + getActiveDeployment() + "?from=" + from + "&to=" + to;
        if (LOG.isDebugEnabled()) {
            LOG.debug("Retrieving record sitemap file {} ", fileName);
        }
        return readController.file(fileName);
    }

    /**
     * Start the sitemap update process
     * @param wskey apikey that verify access to the update procedure
     * @param response
     * @return The index file in plain text
     */
    @RequestMapping(value = "update", method = RequestMethod.GET)
    public String update(@RequestParam(value = "wskey", required = true) String wskey,
                         HttpServletResponse response) throws SiteMapException {
        if (AdminUtils.verifyKey(adminKey, wskey)) {
            response.setStatus(HttpServletResponse.SC_ACCEPTED);
            updateService.update();
            return "Entity sitemap update finished";
        }
        return null;
    }

    /**
     * The active entity sitemap file stores either the value 'blue' or 'green' so we know which deployment of the files we
     * need to retrieve
     * @return
     * @throws SiteMapNotFoundException if the active deployment file was not found
     */
    private String getActiveDeployment() throws SiteMapNotFoundException {
        return readController.file(FileNames.SITEMAP_ENTITY_ACTIVE_FILE);
    }




}
