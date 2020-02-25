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
import eu.europeana.sitemap.config.SitemapConfiguration;
import eu.europeana.sitemap.exceptions.SiteMapException;
import eu.europeana.sitemap.exceptions.SiteMapNotFoundException;
import eu.europeana.sitemap.service.ActiveDeploymentService;
import eu.europeana.sitemap.service.update.UpdateRecordService;
import eu.europeana.sitemap.service.update.UpdateService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

/**
 * Handles requests for record sitemap files from external parties
 * The controller checks which deployment is currently active (blue or green) and retrieves the correct file.
 *
 * @author Patrick Ehlert (major refactoring on 11-02-2019)
 */
@RestController
@RequestMapping("/record")
public class SitemapRecordController extends SitemapAbstractController {

    private SitemapConfiguration config;
    private UpdateService updateService;

    public SitemapRecordController(ActiveDeploymentService activeDeployment, SitemapFileController readController,
                                   UpdateRecordService updateService, SitemapConfiguration config) {
        super(SitemapType.RECORD, activeDeployment, readController);
        this.config = config;
        this.updateService = updateService;
    }

    /**
     * @see SitemapAbstractController#getSitemapIndex()
     */
    @GetMapping(value = {"index"+ Constants.XML_EXTENSION,
            Constants.SITEMAP_RECORD_FILENAME_BASE + Constants.SITEMAP_INDEX_SUFFIX + Constants.XML_EXTENSION},
            produces = MediaType.TEXT_XML_VALUE)
    public String getRecordSitemapIndex() throws SiteMapNotFoundException {
        return super.getSitemapIndex();
    }

    /**
     * @see SitemapAbstractController#getSitemapFile(String, String)
     */
    @GetMapping(value = Constants.SITEMAP_RECORD_FILENAME_BASE + Constants.XML_EXTENSION,
            produces = MediaType.TEXT_XML_VALUE)
    public String getRecordSitemapFile(@RequestParam(value = "from") String from,
                                       @RequestParam(value = "to") String to) throws SiteMapNotFoundException {
        return super.getSitemapFile(from, to);
    }

    /**
     * Start the sitemap update process for records
     * @param wskey apikey that verify access to the update procedure
     * @param response automatically added to method to set response status
     * @return finished message
     */
    @GetMapping(value = "update")
    public String update(@RequestParam(value = "wskey") String wskey,
                         HttpServletResponse response) throws SiteMapException {
        if (AdminUtils.verifyKey(config.getAdminKey(), wskey)) {
            response.setStatus(HttpServletResponse.SC_ACCEPTED);
            // TODO make asynchronous!?
            updateService.update();
            return "Record sitemap update process is done";
        }
        return null;
    }

}
