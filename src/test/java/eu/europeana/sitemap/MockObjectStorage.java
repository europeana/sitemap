package eu.europeana.sitemap;

import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import eu.europeana.features.S3ObjectStorageClient;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

/**
 * This mocks our ObjectStorage library
 */
public class MockObjectStorage {

    private static ObjectListing mockObjectListing = setup(mock(ObjectListing.class));
    private static Map<String, S3Object> storageMap = new HashMap<>();

    // We support limited list functionality with file names only and no pagination
    public static ObjectListing setup(ObjectListing mockListing) {
        when(mockListing.isTruncated()).thenAnswer((Answer<Boolean>) invocation -> false);
        when(mockListing.getObjectSummaries()).thenAnswer((Answer<List<S3ObjectSummary>>) invocation -> {
            List<S3ObjectSummary> result = new ArrayList<>();
            for (Map.Entry<String, S3Object> entry : storageMap.entrySet()) {
                S3ObjectSummary summary = new S3ObjectSummary();
                S3Object file = entry.getValue();
                summary.setKey(file.getKey());
                result.add(summary);
            }
            return result;
        });
        return mockListing;
    }

    public static S3ObjectStorageClient setup(S3ObjectStorageClient mockStorage) {
        when(mockStorage.putObject(anyString(), anyString())).thenAnswer((Answer<String>) invocation -> {
            Object[] args = invocation.getArguments();
            String fileName = (String) args[0];
            String contents = (String) args[1];
            S3Object file = new S3Object();
            file.setObjectContent(new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8)));
            storageMap.put(fileName, file);
            return fileName;
        });
        when(mockStorage.isObjectAvailable(anyString())).thenAnswer((Answer<Boolean>) invocation -> {
            Object[] args = invocation.getArguments();
            String fileName = (String) args[0];
            return storageMap.keySet().contains(fileName);
        });
        when(mockStorage.getObject(anyString())).thenAnswer((Answer<S3Object>) invocation -> {
            Object[] args = invocation.getArguments();
            String fileName = (String) args[0];
            return storageMap.get(fileName);
        });
        when(mockStorage.getObjectContent(anyString())).thenAnswer((Answer<byte[]>) invocation -> {
            Object[] args = invocation.getArguments();
            String fileName = (String) args[0];
            return storageMap.get(fileName).getObjectContent().readAllBytes();
        });
        when(mockStorage.getObjectStream(anyString())).thenAnswer((Answer<InputStream>) invocation -> {
            Object[] args = invocation.getArguments();
            String fileName = (String) args[0];
            return storageMap.get(fileName).getObjectContent().getDelegateStream();
        });
        doAnswer((Answer<Void>) invocation -> {
            String fileName = (String) invocation.getArguments()[0];
            storageMap.remove(fileName);
            return null;
        }).when(mockStorage).deleteObject(anyString());
        when(mockStorage.list()).thenAnswer((Answer<ObjectListing>) invocation -> mockObjectListing);

        return mockStorage;
    }

    /**
     * Empty the mock storage
     */
    public static void clear() {
        storageMap.clear();
    }
}
