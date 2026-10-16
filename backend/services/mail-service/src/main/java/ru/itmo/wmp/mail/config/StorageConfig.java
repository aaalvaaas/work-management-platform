package ru.itmo.wmp.mail.config;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.itmo.wmp.mail.config.properties.StorageProperties;

@Configuration
@RequiredArgsConstructor
public class StorageConfig {
    private final StorageProperties properties;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
            .endpoint(properties.endpoint())
            .credentials(
                properties.accessKey(),
                properties.secretKey()
            )
            .build();
    }
}
