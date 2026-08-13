package ru.itmo.wmp.mail.infrastructure.storage.minio;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.wmp.mail.config.properties.MinioProperties;
import ru.itmo.wmp.mail.exception.StorageException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MinioStorageServiceTest {
    @Mock
    private MinioClient minioClient;

    @Mock
    private MinioProperties properties;

    @InjectMocks
    private MinioStorageService minioStorageService;

    @Test
    void shouldUploadFileSuccessfully() throws Exception {
        String bucket = "test-bucket";
        String filename = "test.txt";
        String contentType = "text/plain";
        byte[] content = "test".getBytes();

        when(properties.bucket())
            .thenReturn(bucket);

        String storageKey = minioStorageService.upload(
            filename,
            new ByteArrayInputStream(content),
            content.length,
            contentType
        );

        assertNotNull(storageKey);
        assertTrue(storageKey.endsWith("-" + filename));

        ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);

        verify(minioClient)
            .putObject(captor.capture());

        PutObjectArgs args = captor.getValue();

        assertEquals(bucket, args.bucket());
        assertEquals(storageKey, args.object());
        assertEquals(contentType, args.contentType());
    }

    @Test
    void shouldDeleteFileSuccessfully() throws Exception {
        String bucket = "test-bucket";
        String storageKey = "test-key";

        when(properties.bucket())
            .thenReturn(bucket);

        minioStorageService.delete(storageKey);

        ArgumentCaptor<RemoveObjectArgs> captor = ArgumentCaptor.forClass(RemoveObjectArgs.class);

        verify(minioClient)
            .removeObject(captor.capture());

        RemoveObjectArgs args = captor.getValue();

        assertEquals(bucket, args.bucket());
        assertEquals(storageKey, args.object());
    }

    @Test
    void shouldThrowExceptionWhenUploadFails() throws Exception {
        String bucket = "test-bucket";
        String filename = "test.txt";
        String contentType = "text/plain";
        byte[] content = "test".getBytes();

        when(properties.bucket())
            .thenReturn(bucket);

        doThrow(new RuntimeException("MinIO unavailable"))
            .when(minioClient)
            .putObject(any(PutObjectArgs.class));

        StorageException exception = assertThrows(
            StorageException.class,
            () -> minioStorageService.upload(
                filename,
                new ByteArrayInputStream(content),
                content.length,
                contentType
            )
        );

        assertTrue(exception.getMessage().contains("Failed to upload file"));

        assertInstanceOf(
            RuntimeException.class,
            exception.getCause()
        );
    }

    @Test
    void shouldThrowExceptionWhenDeleteFails() throws Exception {
        String bucket = "test-bucket";
        String storageKey = "test-key";

        when(properties.bucket())
            .thenReturn(bucket);

        doThrow(new RuntimeException("MinIO unavailable"))
            .when(minioClient)
            .removeObject(any(RemoveObjectArgs.class));

        StorageException exception = assertThrows(
            StorageException.class,
            () -> minioStorageService.delete(storageKey)
        );

        assertTrue(exception.getMessage().contains("Failed to delete file"));

        assertInstanceOf(
            RuntimeException.class,
            exception.getCause()
        );
    }
}
