package eu.europeana.sitemap.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception that is thrown when there is a problem retrieving data from Entity API
 * @author Patrick Ehlert
 * Created on 11-02-2019
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class EntityQueryException extends SiteMapException {

    /**
     * Create new exception when there is a problem retrieving data from Entity API
     * @param msg the error message
     * @param cause the cause of the error
     */
    public EntityQueryException(String msg, Throwable cause) {
        super(msg, cause);
    }

    /**
     * Create new exception when there is a problem retrieving data from Entity API
     * @param msg the error message
     */
    public EntityQueryException(String msg) {
        super(msg);
    }

    /**
     * @return false because we don't want to explicitly log this type of exception
     */
    @Override
    public boolean doLog() {
        return false;
    }
}
