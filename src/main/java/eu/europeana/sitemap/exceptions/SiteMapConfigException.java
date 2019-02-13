package eu.europeana.sitemap.exceptions;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Error that is thrown if there is a problem with the application configuration
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class SiteMapConfigException extends SiteMapException {

    /**
     * Error that is thrown if there is a problem with the application configuration
     * @param msg error message
     */
    public SiteMapConfigException(String msg) {
        super(msg);
    }

    /**
     * Error that is thrown if there is a problem with the application configuration
     * @param msg error message
     * @param t throwable that caused the exception
     */
    public SiteMapConfigException(String msg, Throwable t) {
        super(msg, t);
    }

}
