package eu.europeana.sitemap.service;


import eu.europeana.s3.S3ObjectStorageClient;
import eu.europeana.sitemap.exceptions.SiteMapNotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
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

    private static final Logger LOG = LogManager.getLogger(ReadSitemapServiceImpl.class);

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
        List<FileSummary> fileSummaries = new ArrayList<>();
        do {
            ListObjectsV2Response list = objectStorageProvider.listAll(continuationToken);
            continuationToken = list.nextContinuationToken();
            count = count + list.keyCount();
            // we need to copy the contents to our own list so we can sort it
            for (S3Object file : list.contents()) {
                fileSummaries.add(new FileSummary(file.key(), file.lastModified(), file.size()));
            }
        } while (continuationToken != null);

        fileSummaries.sort(Comparator.comparing(FileSummary::lastModified));
        for (FileSummary fileSummary : fileSummaries) {
            result.write(fileSummary.toString());
        }
        result.write("\n");
        result.write("Total " + count + " files");
        result.flush(); // flush is required here, otherwise we risk returning an incomplete list.
    }

    private static record FileSummary(String id, Instant lastModified, Long size) {

        @Override
        public String toString() {
            StringBuilder s = new StringBuilder();
            s.append(lastModified());
            s.append('\t');
            if (size < KB) {
                s.append(size).append(" bytes");
            } else if (size < MB) {
                s.append(String.format(Locale.getDefault(),
                        "%.2f", size / (double) KB)).append("  KB");
            } else if (size < GB) {
                s.append(String.format(Locale.getDefault(),
                        "%.2f", size / (double) MB)).append("  MB");
            } else {
                s.append(String.format(Locale.getDefault(),
                        "%.2f", size / (double) GB)).append("  GB");
            }
            s.append('\t');
            s.append(id);
            s.append('\n');
            return s.toString();
        }
    }

    /**
     * @see ReadSitemapService#getFileAsStream(String)
     */
    @Override
    public InputStream getFileAsStream(String fileName) throws SiteMapNotFoundException {
        eu.europeana.s3.S3Object file = objectStorageProvider.getObject(fileName);
        if (file == null) {
            throw new SiteMapNotFoundException("File " + fileName + " not found!");
        }
        LOG.debug("Retrieved file {} with content-type {}, last modified {}",
                file.key(), file.getContentType(), file.getLastModified());
        return file.inputStream();
    }
}
