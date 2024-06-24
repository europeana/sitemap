package eu.europeana.sitemap.service;

import eu.europeana.sitemap.exceptions.SiteMapNotFoundException;

import java.io.InputStream;

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
     * @return stream containing the stored file
     * @throws SiteMapNotFoundException thrown when requested file is not available
     */
    InputStream getFileAsStream(String fileName) throws SiteMapNotFoundException;

}
