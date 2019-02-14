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

import eu.europeana.sitemap.SitemapType;
import eu.europeana.sitemap.exceptions.SiteMapNotFoundException;
import eu.europeana.sitemap.service.ActiveDeploymentService;
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
        return super.getSitemapFile(from, to);
    }

}
