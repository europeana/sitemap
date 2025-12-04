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
import eu.europeana.sitemap.StorageFileName;
import eu.europeana.sitemap.exceptions.SiteMapNotFoundException;
import eu.europeana.sitemap.service.ActiveDeploymentService;
import eu.europeana.sitemap.service.Deployment;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;

/**
 * Abstract class that provides the basic controller functionality for handling retrieval requests for
 * the various sitemap types
 */
public abstract class AbstractSitemapController {

    private final SitemapType sitemapType;
    private final ActiveDeploymentService activeDeployment;
    private final SitemapFileController readController;


    protected AbstractSitemapController(SitemapType sitemapType, ActiveDeploymentService activeDeployment, SitemapFileController readController) {
        this.sitemapType = sitemapType;
        this.activeDeployment = activeDeployment;
        this.readController = readController;
    }

    /**
     * Return the sitemap index file
     *
     * @throws SiteMapNotFoundException if the index file wasn't found
     * @return contents of sitemap index file
     */
    public ResponseEntity<InputStreamResource> getSitemapIndex() throws SiteMapNotFoundException {
        Deployment active = activeDeployment.getActiveDeployment(sitemapType);
        String fileName = StorageFileName.getSitemapIndexFileName(sitemapType, active);
        return readController.fileXml(fileName);
    }

    /**
     * Return a sitemap file. Note that the to and from are fixed values, a list of all files with to/from values
     * can be found in the sitemap index file
     *
     * @param from     start index
     * @param to       end index
     * @throws SiteMapNotFoundException if the sitemap file wasn't found
     * @return contents of sitemap file
     */
    public ResponseEntity<InputStreamResource> getSitemapFile(String from, String to) throws SiteMapNotFoundException {
        Deployment active = activeDeployment.getActiveDeployment(sitemapType);
        String appendix = "?from=" + from + "&to=" + to;
        String fileName = StorageFileName.getSitemapFileName(sitemapType, active, appendix);
        return readController.fileXml(fileName);
    }

}
