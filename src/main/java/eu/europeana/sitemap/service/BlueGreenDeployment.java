package eu.europeana.sitemap.service;

/**
 * @author Patrick Ehlert
 * Created on 11-06-2018
 */

public enum BlueGreenDeployment {
    BLUE("blue"), GREEN("green");

    private String name;

    private BlueGreenDeployment(String name) {
        this.name = name;
    }

    public String toString() {
        return this.name;
    }

    public BlueGreenDeployment fromString(String name) {
        if (BLUE.toString().equals(name)) {
            return BLUE;
        } else if (GREEN.toString().equals(name)) {
            return GREEN;
        }
        return null;
    }
}
