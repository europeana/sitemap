package eu.europeana.sitemap.service;

import eu.europeana.sitemap.exceptions.SiteMapException;
import eu.europeana.sitemap.exceptions.SiteMapNotFoundException;

import java.io.IOException;

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
    void update() throws SiteMapException;

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
     * @throws IOException thrown when there is an error reading the file
     */
    String getFileContent(String fileName) throws SiteMapNotFoundException, IOException;

    /**
     * Retrieve the (currently active instance of the) sitemap index file
     * @return active index file as a string
     * @throws SiteMapNotFoundException thrown when requested file is not available
     * @throws IOException thrown when there is an error reading the file
     */
    String getIndexFileContent() throws SiteMapNotFoundException, IOException;
}
