package eu.europeana.sitemap.service;


import com.amazonaws.services.s3.model.S3ObjectSummary;
import eu.europeana.features.S3ObjectStorageClient;
import eu.europeana.sitemap.exceptions.SiteMapNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Read sitemap files directly from the object storage provider
 *
 * @author Patrick Ehlert on 11-9-17.
 */
@Service
public class ReadSitemapServiceImpl implements ReadSitemapService {

    private static final int KB = 1024;
    private static final int MB = 1024 * KB;
    private static final int GB = 1024 * MB;

    private final S3ObjectStorageClient objectStorageProvider;

    @Autowired
    public ReadSitemapServiceImpl (S3ObjectStorageClient objectStorageProvider) {
        this.objectStorageProvider = objectStorageProvider;
    }

    /**
     * @see ReadSitemapService#getFiles()
     */
    @Override
    public String getFiles() {
        List<S3ObjectSummary> files = objectStorageProvider.listAll();
        Collections.sort(files, Comparator.comparing(S3ObjectSummary::getLastModified));
        StringBuilder result = new StringBuilder();
        for (S3ObjectSummary file : files) {
            result.append(file.getLastModified());
            result.append('\t');
            if (file.getSize() < KB) {
                result.append(file.getSize()).append(" bytes");
            } else if (file.getSize() < MB) {
                result.append(String.format("%.2d", file.getSize() / (double) KB)).append("  KB");
            } else if (file.getSize() < GB) {
                result.append(String.format("%.2d", file.getSize() / (double) MB)).append("  MB");
            } else {
                result.append(String.format("%.2d", file.getSize() / (double) GB)).append("  GB");
            }
            result.append('\t');
            result.append(file.getKey());
            result.append('\n');
        }
        return result.toString();
    }

    /**
     * @see ReadSitemapService#getFileAsStream(String)
     */
    @Override
    public InputStream getFileAsStream(String fileName) throws SiteMapNotFoundException {
        InputStream result = objectStorageProvider.getObjectStream(fileName);
        if (result == null) {
            throw new SiteMapNotFoundException("File " + fileName + " not found!");
        }
        return result;
    }
}
