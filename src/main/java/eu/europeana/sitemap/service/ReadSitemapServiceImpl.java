package eu.europeana.sitemap.service;


import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import eu.europeana.features.S3ObjectStorageClient;
import eu.europeana.sitemap.exceptions.SiteMapNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
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
            ListObjectsV2Result list = objectStorageProvider.listAll(continuationToken);
            continuationToken = list.getNextContinuationToken();
            count = count + list.getKeyCount();
            List<S3ObjectSummary> files = list.getObjectSummaries();
            Collections.sort(files, Comparator.comparing(S3ObjectSummary::getLastModified));
            // sorting may be incorrect because of pagination, but we set a large page size so should be fairly okay

            StringBuilder s = new StringBuilder();
            for (S3ObjectSummary file : files) {
                s.append(file.getLastModified());
                s.append('\t');
                if (file.getSize() < KB) {
                    s.append(file.getSize()).append(" bytes");
                } else if (file.getSize() < MB) {
                    s.append(String.format(Locale.getDefault(),
                            "%.2f", file.getSize() / (double) KB)).append("  KB");
                } else if (file.getSize() < GB) {
                    s.append(String.format(Locale.getDefault(),
                            "%.2f", file.getSize() / (double) MB)).append("  MB");
                } else {
                    s.append(String.format(Locale.getDefault(),
                            "%.2f", file.getSize() / (double) GB)).append("  GB");
                }
                s.append('\t');
                s.append(file.getKey());
                s.append('\n');
            }
            result.write(s.toString());
        } while (continuationToken != null);
        result.write("\n");
        result.write("Total " + count + " files");
        result.flush(); // flush is requires here, otherwise we risk returning an incomplete list.
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
