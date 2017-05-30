package eu.europeana.sitemap.service;

/**
 * All supported sitemap methods
 *
 * Created by ymamakis on 11/16/15.
 */
public interface SitemapService {

    /**
     * Start the sitemap update process. This will delete any old sitemap at the inactive blue/green instance first,
     * then create a new sitemap and finally switching to the blue/green instances.
     */
    void update();
}
