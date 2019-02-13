package eu.europeana.sitemap;

import eu.europeana.domain.StorageObject;
import eu.europeana.features.ObjectStorageClient;
import org.jclouds.io.Payload;
import org.jclouds.io.payloads.ByteArrayPayload;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.*;

import static org.mockito.Mockito.*;

/**
 * This mocks our ObjectStorage library
 */
public class MockObjectStorage {

    private static Map<String, Payload> storageMap = new HashMap<>();

    public static ObjectStorageClient setup(ObjectStorageClient mockStorage) {
        when(mockStorage.put(anyString(), any())).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                String fileName = (String) args[0];
                Payload payload = (Payload) args[1];
                storageMap.put(fileName, payload);
                return fileName;
            }
        });
        when(mockStorage.isAvailable(anyString())).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                String fileName = (String) args[0];
                return storageMap.keySet().contains(fileName);
            }
        });
        when(mockStorage.getContent(anyString())).thenAnswer(new Answer<byte[]>() {
            @Override
            public byte[] answer(InvocationOnMock invocation)  {
                Object[] args = invocation.getArguments();
                String fileName = (String) args[0];
                Payload result = storageMap.get(fileName);
                if (result != null) {
                    return ((ByteArrayPayload) result).getRawContent();
                }
                return null;
            }
        });
        when(mockStorage.get(anyString())).thenAnswer(new Answer<Optional<StorageObject>>() {
            @Override
            public Optional<StorageObject> answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                String fileName = (String) args[0];
                Payload payload = storageMap.get(fileName);
                if (payload != null) {
                    return Optional.of(new StorageObject(fileName, null, null, payload));
                }
                return Optional.empty();
            }
        });
        when(mockStorage.list()).thenAnswer(new Answer<List<StorageObject>>() {
            @Override
            public List<StorageObject> answer(InvocationOnMock invocation) {
                List<StorageObject> result = new ArrayList<>();
                for (Map.Entry<String, Payload> entry : storageMap.entrySet()) {
                    result.add(new StorageObject(entry.getKey(), null, null, entry.getValue()));
                }
                return result;
            }
        });
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                String fileName = (String) invocation.getArguments()[0];
                storageMap.remove(fileName);
                return null;
            }
        }).when(mockStorage).delete(anyString());

        return mockStorage;
    }

    /**
     * Empty the mock storage
     */
    public static void clear() {
        storageMap.clear();
    }
}
