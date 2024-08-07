package eu.europeana.sitemap.web;

import org.apache.commons.lang3.StringUtils;

/**
 * Utility class to check if a provided key is valid.
 *
 * @author Patrick Ehlert
 * Created on 14-06-2018
 */
public final class AdminUtils {

    private AdminUtils() {
        // empty constructor to prevent initialization
    }

    /**
     * Checks if the provided adminKey is not empty and if the provided wskey matches the provided adminKey. If not
     * a SecurityException is thrown.
     * @param adminKey administrator key (at the moment we only support one)
     * @param wskey the provided
     * @return true if the provided wskey is the same as the adminKey
     * @throws SecurityException when provided wskey is not the same as the adminKey
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
