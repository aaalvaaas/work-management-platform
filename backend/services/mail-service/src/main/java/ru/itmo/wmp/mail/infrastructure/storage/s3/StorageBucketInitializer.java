package ru.itmo.wmp.mail.infrastructure.storage.s3;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.itmo.wmp.mail.config.properties.StorageProperties;
import ru.itmo.wmp.mail.exception.StorageException;

@Component
@RequiredArgsConstructor
public class StorageBucketInitializer {
    private final MinioClient minioClient;
    private final StorageProperties properties;

    @PostConstruct
    public void init() {
        try {
            boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder()
                    .bucket(properties.bucket())
                    .build()
            );

            if (!exists) {
                minioClient.makeBucket(
                    MakeBucketArgs.builder()
                        .bucket(properties.bucket())
                        .build()
                );
            }
        } catch (Exception e) {
            throw new StorageException("Failed to initialize storage bucket", e);
        }
    }
}
