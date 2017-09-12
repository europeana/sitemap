package eu.europeana.sitemap.exceptions;

/**
 * Base exception for the sitemap application
 * @author Patrick Ehlert on 12-9-17.
 */
public class SiteMapException extends Exception {

    /**
     * General error thrown by the sitemap application
     * @param s
     */
    public SiteMapException(String s) {
        super(s);
    }

    /**
     * General error thrown by the sitemap application
     * @param s
     * @param t
     */
    public SiteMapException(String s, Throwable t) {
        super(s, t);
    }
}
