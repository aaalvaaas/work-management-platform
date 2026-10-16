package ru.itmo.wmp.mail;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class IntegrationTestContainers {
    private static final String TEST_BUCKET = "mail-attachments-test";

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:17");
    }

    @Bean
    MinIOContainer minioContainer() {
        return new MinIOContainer("minio/minio:RELEASE.2025-07-23T15-54-02Z");
    }

    @Bean
    DynamicPropertyRegistrar minioPropertiesRegistrar(MinIOContainer minioContainer) {
        return registry -> {
            registry.add("minio.endpoint", minioContainer::getS3URL);
            registry.add("minio.access-key", minioContainer::getUserName);
            registry.add("minio.secret-key", minioContainer::getPassword);
            registry.add("minio.bucket", () -> TEST_BUCKET);
        };
    }
}
