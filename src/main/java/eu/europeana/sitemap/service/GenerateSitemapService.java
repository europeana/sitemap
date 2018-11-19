package eu.europeana.sitemap.service;

import eu.europeana.sitemap.exceptions.SiteMapException;

/**
 * All supported methods for generating a new sitemap
 *
 * Created by ymamakis on 11/16/15.
 */
public interface GenerateSitemapService {

    /**
     * Start the sitemap update process. This will delete any old sitemap at the inactive blue/green instance first,
     * then create a new sitemap and finally switching to the blue/green instances.
     */
    void update() throws SiteMapException;

}
