package eu.europeana.sitemap.web;

import org.apache.commons.lang.StringUtils;

/**
 * Utility class to check if a provided key is valid.
 *
 * @author Patrick Ehlert
 * Created on 14-06-2018
 */
public class AdminUtils {

    private AdminUtils() {
        // empty constructor to prevent initialization
    }

    /**
     * Checks if the provided adminKey is not empty and if the provided wskey matches the provided adminKey. If not
     * a SecurityException is thrown.
     */
    public static boolean verifyKey(String adminKey, String wskey) throws SecurityException {
        if (StringUtils.isEmpty(adminKey)) {
            throw new SecurityException("No updates are allowed");
        } else if (!adminKey.equals(wskey)) {
            throw new SecurityException("Invalid key");
        }
        return true;
    }
}
