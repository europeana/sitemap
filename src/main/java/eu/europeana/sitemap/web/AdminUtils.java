package eu.europeana.sitemap.web;

import eu.europeana.sitemap.exceptions.InvalidApiKeyException;
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
     * an InvalidApiKeyException is thrown.
     * @param adminKey administrator key (at the moment we only support one)
     * @param wskey the provided
     * @return true if the provided wskey is the same as the adminKey
     * @throws InvalidApiKeyException when provided wskey is not the same as the adminKey
     */
    public static boolean verifyKey(String adminKey, String wskey) throws InvalidApiKeyException {
        if (StringUtils.isEmpty(adminKey)) {
            throw new InvalidApiKeyException("No updates are allowed");
        } else if (!adminKey.equals(wskey)) {
            throw new InvalidApiKeyException("Invalid key");
        }
        return true;
    }
}
