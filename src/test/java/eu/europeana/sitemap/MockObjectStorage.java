package eu.europeana.sitemap;


import eu.europeana.s3.S3Object;
import eu.europeana.s3.S3ObjectStorageClient;
import org.mockito.stubbing.Answer;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.*;

import static org.mockito.Mockito.*;

/**
 * This mocks an S3 object storage.
 */
public class MockObjectStorage {

    private static final Map<String, S3Object> storageMap = new HashMap<>();

    public static S3ObjectStorageClient setup(S3ObjectStorageClient mockStorage) {
        // simple mocking of listAll, we don't really support continuationTokens or maxPageSize
        when(mockStorage.listAll(any())).thenAnswer((Answer< ListObjectsV2Response>) invocation ->
                ListObjectsV2Response.builder().contents(
                        convertToS3Objects(storageMap.values()))
                        .keyCount(storageMap.size())
                        .build()
        );
        when(mockStorage.listAll(any(), any(Integer.class))).thenAnswer((Answer<ListObjectsV2Response>) invocation ->
                ListObjectsV2Response.builder().contents(
                        convertToS3Objects(storageMap.values()))
                        .keyCount(storageMap.size())
                        .build()
        );
        when(mockStorage.putObject(anyString(), anyString(), any(byte[].class))).thenAnswer((Answer<String>) invocation -> {
            Object[] args = invocation.getArguments();
            String id = (String) args[0];
            String contentType = (String) args[1];
            byte[] content = (byte[]) args[2];
            // generate some fake metadata
            Map<String, Object> metadata = new HashMap<>();
            String eTag = UUID.randomUUID().toString();
            metadata.put(S3Object.ETAG, eTag);
            metadata.put(S3Object.LAST_MODIFIED, Instant.now());
            metadata.put(S3Object.CONTENT_TYPE, contentType);
            metadata.put(S3Object.CONTENT_LENGTH, content.length);
            storageMap.put(id, new S3Object(id, new ByteArrayInputStream(content), metadata));
            return eTag;
        });
        when(mockStorage.isObjectAvailable(anyString())).thenAnswer((Answer<Boolean>) invocation -> {
            Object[] args = invocation.getArguments();
            String id = (String) args[0];
            return storageMap.containsKey(id);
        });
        when(mockStorage.getObject(anyString())).thenAnswer((Answer<S3Object>) invocation -> {
            Object[] args = invocation.getArguments();
            String id = (String) args[0];
            return storageMap.get(id);
        });
        when(mockStorage.getObjectAsBytes(anyString())).thenAnswer((Answer<byte[]>) invocation -> {
            Object[] args = invocation.getArguments();
            String fileName = (String) args[0];
            return storageMap.get(fileName).inputStream().readAllBytes();
        });
        when(mockStorage.getObjectAsStream(anyString())).thenAnswer((Answer<InputStream>) invocation -> {
            Object[] args = invocation.getArguments();
            String fileName = (String) args[0];
            return storageMap.get(fileName).inputStream();
        });
        doAnswer((Answer<Void>) invocation -> {
            String fileName = (String) invocation.getArguments()[0];
            storageMap.remove(fileName);
            return null;
        }).when(mockStorage).deleteObject(anyString());

        return mockStorage;
    }

    /**
     * Empty the mock storage
     */
    public static void clear() {
        storageMap.clear();
    }

    private static software.amazon.awssdk.services.s3.model.S3Object convertToS3Summary(S3Object s3Object) {
        return software.amazon.awssdk.services.s3.model.S3Object.builder()
                .key(s3Object.key())
                .eTag(s3Object.getETag())
                .size(s3Object.getContentLength())
                .lastModified(s3Object.getLastModified())
                .build();
    }

    private static List<software.amazon.awssdk.services.s3.model.S3Object> convertToS3Objects(Collection<S3Object> s3Objects)  {
        List<software.amazon.awssdk.services.s3.model.S3Object> list = new ArrayList<>();
        for (S3Object s3Object : s3Objects) {
            list.add(convertToS3Summary(s3Object));
        }
        return list;
    }

}
