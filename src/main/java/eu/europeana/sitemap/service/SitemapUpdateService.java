package eu.europeana.sitemap.service;

import eu.europeana.sitemap.exceptions.SiteMapException;

/**
 * @author Patrick Ehlert
 *         <p>
 *         Created on 14-06-2018
 */
public interface SitemapUpdateService {

    public void update() throws SiteMapException;
}
