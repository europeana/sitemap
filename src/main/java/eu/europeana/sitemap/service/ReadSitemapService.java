package eu.europeana.sitemap.service;

import eu.europeana.sitemap.exceptions.SiteMapNotFoundException;

/**
 * All supported reading sitemap methods
 *
 * @author Patrick Ehlert on 11-9-17.
 */
public interface ReadSitemapService {

    /**
     * Retrieve a list of all files that are stored at our object provider's bucket
     * @return list of stored sitemap files
     */
    String getFiles();

    /**
     * Retrieve the contents of a particular file stored at our object provider's bucket
     * @param fileName the name of the requested file
     * @return contents of stored sitemap file in xml
     * @throws SiteMapNotFoundException thrown when requested file is not available
     */
    String getFileContent(String fileName) throws SiteMapNotFoundException;

    /**
     * Retrieve the (currently active instance of the) sitemap index file
     * @return active index file as a string
     * @throws SiteMapNotFoundException thrown when requested file is not available
     */
    String getIndexFileContent() throws SiteMapNotFoundException;
}
