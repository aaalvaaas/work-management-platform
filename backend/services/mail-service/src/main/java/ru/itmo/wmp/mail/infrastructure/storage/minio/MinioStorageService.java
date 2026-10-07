package ru.itmo.wmp.mail.infrastructure.storage.minio;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.itmo.wmp.mail.application.storage.StorageService;
import ru.itmo.wmp.mail.config.properties.MinioProperties;
import ru.itmo.wmp.mail.exception.StorageException;

import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MinioStorageService implements StorageService {
    private final MinioClient minioClient;
    private final MinioProperties properties;

    @Override
    public String upload(String filename, InputStream inputStream, long size, String contentType) {
        String storageKey = generateStorageKey(filename);

        try {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(storageKey)
                    .stream(
                        inputStream,
                        size,
                        -1
                    )
                    .contentType(contentType)
                    .build()
            );

            return storageKey;
        } catch (Exception e) {
            throw new StorageException("Failed to upload file: " + filename, e);
        }
    }

    @Override
    public InputStream download(String storageKey) {
        try {
            return minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(storageKey)
                    .build()
            );
        } catch (Exception e) {
            throw new StorageException("Failed to download file: " + storageKey, e);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(storageKey)
                    .build()
            );
        } catch (Exception e) {
            throw new StorageException("Failed to delete file: " + storageKey, e);
        }
    }

    private String generateStorageKey(String filename) {
        return UUID.randomUUID() + "-" + filename;
    }
}
