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

    private static final Map<String, S3Object> storageMap = new HashMap<>();

    public static S3ObjectStorageClient setup(S3ObjectStorageClient mockStorage) {
        when(mockStorage.listAll(any())).thenAnswer((Answer<ListObjectsV2Result>) invocation ->
                new ListObjectsV2ResultMock(storageMap)
        );
        when(mockStorage.listAll(any(), anyInt())).thenAnswer((Answer<ListObjectsV2Result>) invocation ->
                new ListObjectsV2ResultMock(storageMap)
        );
        when(mockStorage.putObject(anyString(), any())).thenAnswer((Answer<String>) invocation -> {
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

        return mockStorage;
    }

    /**
     * Empty the mock storage
     */
    public static void clear() {
        storageMap.clear();
    }


    private static final class ListObjectsV2ResultMock extends ListObjectsV2Result {

        private List<S3ObjectSummary> objectSummariesMock;

        public ListObjectsV2ResultMock(Map<String, S3Object> storageMap) {
            objectSummariesMock = new ArrayList();
            for (Map.Entry<String, S3Object> entry : storageMap.entrySet()) {
                S3ObjectSummary summary = new S3ObjectSummary();
                summary.setKey(entry.getKey());
                summary.setSize(100); // fake number, just to have some data
                summary.setLastModified(new Date()); // fake date, just to have some data
                objectSummariesMock.add(summary);
            }
        }

        @Override
        public List<S3ObjectSummary> getObjectSummaries() {
            return this.objectSummariesMock;
        }

        @Override
        public String getNextContinuationToken() {
            return null;
        }

        @Override
        public int getKeyCount() {
            return this.objectSummariesMock.size();
        }
    }

}
