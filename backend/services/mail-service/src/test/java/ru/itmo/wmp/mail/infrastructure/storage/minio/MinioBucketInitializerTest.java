package ru.itmo.wmp.mail.infrastructure.storage.minio;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.wmp.mail.config.properties.MinioProperties;
import ru.itmo.wmp.mail.exception.StorageException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MinioBucketInitializerTest {
    @Mock
    private MinioClient minioClient;

    @Mock
    private MinioProperties properties;

    @InjectMocks
    private MinioBucketInitializer minioBucketInitializer;

    @Test
    void shouldNotCreateBucketWhenBucketAlreadyExists() throws Exception {
        String bucket = "test-bucket";

        when(properties.bucket())
            .thenReturn(bucket);

        when(minioClient.bucketExists(any(BucketExistsArgs.class)))
            .thenReturn(true);

        minioBucketInitializer.init();

        verify(minioClient)
            .bucketExists(any(BucketExistsArgs.class));

        verify(minioClient, never())
            .makeBucket(any());
    }

    @Test
    void shouldCreateBucketWhenBucketDoesNotExists() throws Exception {
        String bucket = "test-bucket";

        when(properties.bucket())
            .thenReturn(bucket);

        when(minioClient.bucketExists(any(BucketExistsArgs.class)))
            .thenReturn(false);

        minioBucketInitializer.init();

        verify(minioClient)
            .bucketExists(any(BucketExistsArgs.class));

        verify(minioClient)
            .makeBucket(any());
    }

    @Test
    void shouldUseConfiguredBucketName() throws Exception {
        String bucket = "test-bucket";

        when(properties.bucket())
            .thenReturn(bucket);

        when(minioClient.bucketExists(any(BucketExistsArgs.class)))
            .thenReturn(true);

        ArgumentCaptor<BucketExistsArgs> captor = ArgumentCaptor.forClass(BucketExistsArgs.class);

        minioBucketInitializer.init();

        verify(minioClient)
            .bucketExists(captor.capture());

        assertEquals(bucket, captor.getValue().bucket());
    }

    @Test
    void shouldThrowExceptionWhenInitializationFails() throws Exception {
        String bucket = "test-bucket";

        when(properties.bucket())
            .thenReturn(bucket);

        doThrow(new RuntimeException("MinIO unavailable"))
            .when(minioClient)
            .bucketExists(any(BucketExistsArgs.class));

        StorageException exception = assertThrows(
            StorageException.class,
            () -> minioBucketInitializer.init()
        );

        assertTrue(exception.getMessage().contains("Failed to initialize MinIO bucket"));

        assertInstanceOf(
            RuntimeException.class,
            exception.getCause()
        );

        verify(minioClient, never())
            .makeBucket(any());
    }
}
