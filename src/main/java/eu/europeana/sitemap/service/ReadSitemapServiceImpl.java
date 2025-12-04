package eu.europeana.sitemap.service;


import eu.europeana.s3.S3ObjectStorageClient;
import eu.europeana.sitemap.exceptions.SiteMapNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

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

    /**
     * Initialize a new service for reading sitemap files
     * @param objectStorageProvider autowired storage provider that contains the sitemap files
     */
    @Autowired
    public ReadSitemapServiceImpl (S3ObjectStorageClient objectStorageProvider) {
        this.objectStorageProvider = objectStorageProvider;
    }

    /**
     * @see ReadSitemapService#getFilesAsStream(OutputStream out)
     */
    @Override
    public void getFilesAsStream(OutputStream out) throws IOException {
        OutputStreamWriter result = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        long count = 0;
        String continuationToken = null;
        do {
            ListObjectsV2Response list = objectStorageProvider.listAll(continuationToken);
            continuationToken = list.nextContinuationToken();
            count = count + list.keyCount();
            List<S3Object> files = list.contents();
            files.sort(Comparator.comparing(S3Object::lastModified));
            // sorting may be incorrect because of pagination, but we set a large page size so should be fairly okay

            StringBuilder s = new StringBuilder();
            for (S3Object file : files) {
                s.append(file.lastModified());
                s.append('\t');
                if (file.size() < KB) {
                    s.append(file.size()).append(" bytes");
                } else if (file.size() < MB) {
                    s.append(String.format(Locale.getDefault(),
                            "%.2f", file.size() / (double) KB)).append("  KB");
                } else if (file.size() < GB) {
                    s.append(String.format(Locale.getDefault(),
                            "%.2f", file.size() / (double) MB)).append("  MB");
                } else {
                    s.append(String.format(Locale.getDefault(),
                            "%.2f", file.size() / (double) GB)).append("  GB");
                }
                s.append('\t');
                s.append(file.key());
                s.append('\n');
            }
            result.write(s.toString());
        } while (continuationToken != null);
        result.write("\n");
        result.write("Total " + count + " files");
        result.flush(); // flush is required here, otherwise we risk returning an incomplete list.
    }

    /**
     * @see ReadSitemapService#getFileAsStream(String)
     */
    @Override
    public InputStream getFileAsStream(String fileName) throws SiteMapNotFoundException {
        InputStream result = objectStorageProvider.getObjectAsStream(fileName);
        if (result == null) {
            throw new SiteMapNotFoundException("File " + fileName + " not found!");
        }
        return result;
    }
}
