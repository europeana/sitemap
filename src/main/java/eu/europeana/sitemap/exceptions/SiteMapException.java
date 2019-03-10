package eu.europeana.sitemap.exceptions;

/**
 * Base exception for the sitemap application
 * @author Patrick Ehlert on 12-9-17.
 */
public class SiteMapException extends Exception {

    /**
     * General error thrown by the sitemap application
     * @param s error message
     */
    public SiteMapException(String s) {
        super(s);
    }

    /**
     * General error thrown by the sitemap application
     * @param s error message
     * @param t throwable that caused the exception
     */
    public SiteMapException(String s, Throwable t) {
        super(s, t);
    }

    /**
     * @return boolean indicating whether this type of exception should be logged or not
     */
    public boolean doLog() {
        return true; // default we log all exceptions
    }
}
