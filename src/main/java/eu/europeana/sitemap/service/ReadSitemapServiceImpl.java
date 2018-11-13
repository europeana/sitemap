package eu.europeana.sitemap.service;


import eu.europeana.domain.StorageObject;
import eu.europeana.features.ObjectStorageClient;
import eu.europeana.sitemap.FileNames;
import eu.europeana.sitemap.exceptions.SiteMapNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.jboss.logging.Logger;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Read sitemap files directly from the object storage provider
 *
 * @author Patrick Ehlert on 11-9-17.
 */
//@Service
public class ReadSitemapServiceImpl implements ReadSitemapService {

    private final ObjectStorageClient objectStorageProvider;

    public ReadSitemapServiceImpl (ObjectStorageClient objectStorageProvider) {
        this.objectStorageProvider = objectStorageProvider;
        LogManager.getLogger(ReadSitemapServiceImpl.class).debug("init");
    }

    /**
     * @see ReadSitemapService#getFiles()
     */
    @Override
    public String getFiles() {
        List<StorageObject> files = objectStorageProvider.list();
        Collections.sort(files, (StorageObject o1, StorageObject o2) -> o1.getLastModified().compareTo(o2.getLastModified()));
        StringBuilder result = new StringBuilder();
        for (StorageObject file : files) {
            result.append(file.getLastModified());
            result.append('\t');
            result.append(file.getName());
            result.append('\n');
        }
        return result.toString();
    }

    /**
     * @see ReadSitemapService#getFileContent(String)
     */
    @Override
    public String getFileContent(String fileName) throws SiteMapNotFoundException {
        Optional<StorageObject> file = objectStorageProvider.get(fileName);
        if (file.isPresent()) {
            return new String(objectStorageProvider.getContent(fileName), StandardCharsets.UTF_8);
        }
        throw new SiteMapNotFoundException("File " + fileName + " not found!");
    }

}
