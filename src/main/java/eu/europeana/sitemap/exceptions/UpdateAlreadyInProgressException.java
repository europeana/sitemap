package eu.europeana.sitemap.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception that indicates that there is already an update running
 * Created by ymamakis on 11/16/15.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class UpdateAlreadyInProgressException extends SiteMapException {

    public UpdateAlreadyInProgressException(String msg) {
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
