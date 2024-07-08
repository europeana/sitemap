package eu.europeana.sitemap.service.update;

import eu.europeana.sitemap.SitemapType;
import eu.europeana.sitemap.exceptions.SiteMapException;

/**
 * Update interface for classes that support updating a particular sitemap type
 * @author Patrick Ehlert
 * Created on 14-06-2018
 */
public interface UpdateService {

    /**
     * Triggers a sitemap update
     * @throws SiteMapException when there is an error during the update process
     */
    public void update() throws SiteMapException;

    /**
     * @return the type of the sitemap that should be updated
     */
    public SitemapType getSitemapType();

}
