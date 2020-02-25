package eu.europeana.sitemap.service;

import org.apache.logging.log4j.LogManager;

/**
 * Enumeration of deployment types. Either blue and green deployment is active
 * @author Patrick Ehlert
 * Created on 11-06-2018
 */

public enum Deployment {
    BLUE("blue"), GREEN("green");

    private String name;

    Deployment(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }

    public static Deployment fromString(String name) {
        if (BLUE.toString().equals(name)) {
            return BLUE;
        } else if (GREEN.toString().equals(name)) {
            return GREEN;
        }
        LogManager.getLogger(Deployment.class).warn("Unknown deployment type: {} ", name);
        return null;
    }
}
