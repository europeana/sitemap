package eu.europeana.sitemap;

import java.util.Locale;

public class XmlUtils {

    /**
     * Removes all newlines, tabs and multiple spacing so we return 1 single line of xml. This is so we can do easy
     * 'contains()' checks in unit tests
     * @param xml input xml
     * @return xml as 1 line with newlines, tabs and multiple spacing removed
     */
    public static String harmonizeXml(String xml) {
        return xml.replaceAll("[\\n\\r\\t]", "").replaceAll("\\s+", " ").trim().toLowerCase(Locale.getDefault());
    }
}
