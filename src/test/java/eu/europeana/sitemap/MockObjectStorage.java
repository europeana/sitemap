package eu.europeana.sitemap;

import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import eu.europeana.features.S3ObjectStorageClient;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.mockito.Mockito.*;

/**
 * This mocks our ObjectStorage library
 */
public class MockObjectStorage {

    private static Map<String, S3Object> storageMap = new HashMap<>();

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
        when(mockStorage.listAll(anyString())).thenAnswer((Answer<ListObjectsV2Result>) invocation -> {
            return new ListObjectsV2ResultMock();
        });
        when(mockStorage.listAll(anyString(), anyInt())).thenAnswer((Answer<ListObjectsV2Result>) invocation -> {
            return new ListObjectsV2ResultMock();
        });
        doAnswer((Answer<Void>) invocation -> {
            String fileName = (String) invocation.getArguments()[0];
            storageMap.remove(fileName);
            return null;
        }).when(mockStorage).deleteObject(anyString());

        return mockStorage;
    }


    private static final class ListObjectsV2ResultMock extends ListObjectsV2Result {

        private List<S3ObjectSummary> objectSummariesMock;

        public ListObjectsV2ResultMock() {
            objectSummariesMock = new ArrayList();
            for (Map.Entry<String, S3Object> entry : storageMap.entrySet()) {
                S3ObjectSummary summary = new S3ObjectSummary();
                summary.setKey(entry.getKey());
                summary.setSize(100); // fake number, just to have some data
                summary.setLastModified(new Date()); // fake date, just to have some data
                objectSummariesMock.add(summary);
            }
        }

        public List<S3ObjectSummary> getObjectSummaries() {
            return this.objectSummariesMock;
        }

        public String getNextContinuationToken() {
            return null;
        }

        public int getKeyCount() {
            return storageMap.size();
        }
    }

    /**
     * Empty the mock storage
     */
    public static void clear() {
        storageMap.clear();
    }
}
