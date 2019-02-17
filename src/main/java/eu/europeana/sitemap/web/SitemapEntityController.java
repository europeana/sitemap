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

import eu.europeana.sitemap.Constants;
import eu.europeana.sitemap.SitemapType;
import eu.europeana.sitemap.exceptions.SiteMapException;
import eu.europeana.sitemap.exceptions.SiteMapNotFoundException;
import eu.europeana.sitemap.service.ActiveDeploymentService;
import eu.europeana.sitemap.service.update.UpdateEntityService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
public class SitemapEntityController extends SitemapAbstractController {

    private UpdateEntityService updateService;

    @Value("${admin.apikey}")
    private String adminKey;

    public SitemapEntityController(ActiveDeploymentService activeDeployment, SitemapFileController readController,
                                   UpdateEntityService updateService) {
        super(SitemapType.ENTITY, activeDeployment, readController);
        this.updateService = updateService;
    }

    /**
     * @see SitemapAbstractController#getSitemapIndex()
     */
    @GetMapping(value = {"index",
            Constants.SITEMAP_ENTITY_FILENAME_BASE + Constants.SITEMAP_INDEX_SUFFIX + Constants.XML_EXTENSION})
    public String getEntitySitemapIndex() throws SiteMapNotFoundException {
        return super.getSitemapIndex();
    }

    /**
     * @see SitemapAbstractController#getSitemapFile(String, String)
     */
    @GetMapping(value = Constants.SITEMAP_ENTITY_FILENAME_BASE + Constants.XML_EXTENSION)
    public String getEntitySitemapFile(@RequestParam(value = "from") String from,
                                       @RequestParam(value = "to") String to) throws SiteMapNotFoundException {
        return super.getSitemapFile(from, to);
    }

    /**
     * Start the sitemap update process for entities
     * @param wskey apikey that verify access to the update procedure
     * @param response automatically added to method to set response status
     * @return The entity index file in plain text
     */
    @GetMapping(value = "update")
    public void update(@RequestParam(value = "wskey") String wskey,
                         HttpServletResponse response) throws SiteMapException {
        if (AdminUtils.verifyKey(adminKey, wskey)) {
            response.setStatus(HttpServletResponse.SC_ACCEPTED);
            updateService.update();
        }
    }

}
