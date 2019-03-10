package eu.europeana.sitemap.exceptions;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Error thrown when a requested sitemap file cannot be found/retrieved
 * Created by jeroen on 23-12-16.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class SiteMapNotFoundException extends SiteMapException {

    /**
     * Error thrown when a requested sitemap file cannot be found/retrieved
     * @param s error message
     */
    public SiteMapNotFoundException(String s) {
        super(s);
    }

    /**
     * @return false because we don't want to explicitly log this type of exception
     */
    @Override
    public boolean doLog() {
        return false;
    }
}
