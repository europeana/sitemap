package eu.europeana.sitemap.exceptions;


/**
 * Error thrown when a requested sitemap file cannot be found/retrieved
 * Created by jeroen on 23-12-16.
 */
public class SiteMapNotFoundException extends SiteMapException {

    /**
     * Error thrown when a requested sitemap file cannot be found/retrieved
     * @param s
     */
    public SiteMapNotFoundException(String s) {
        super(s);
    }
}
