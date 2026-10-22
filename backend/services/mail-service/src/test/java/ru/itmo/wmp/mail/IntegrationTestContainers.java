package ru.itmo.wmp.mail;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.containers.wait.strategy.Wait;

@TestConfiguration(proxyBeanMethods = false)
public class IntegrationTestContainers {
    private static final String TEST_BUCKET = "mail-attachments-test";
    private static final int S3_PORT = 9090;

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:17");
    }

    @Bean
    @ServiceConnection
    MongoDBContainer mongoContainer() {
        return new MongoDBContainer("mongo:8.0");
    }

    @Bean
    @ServiceConnection
    KafkaContainer kafkaContainer() {
        return new KafkaContainer("apache/kafka:3.9.1");
    }

    @Bean
    GenericContainer<?> objectStorageContainer() {
        return new GenericContainer<>("adobe/s3mock:5.2.3")
            .withExposedPorts(S3_PORT)
            .withEnv("initialBuckets", TEST_BUCKET)
            .waitingFor(Wait.forHttp("/favicon.ico").forPort(S3_PORT).forStatusCodeMatching(status -> true));
    }

    @Bean
    DynamicPropertyRegistrar objectStoragePropertiesRegistrar(GenericContainer<?> objectStorageContainer) {
        return registry -> {
            registry.add(
                "storage.endpoint",
                () -> "http://%s:%d".formatted(
                    objectStorageContainer.getHost(),
                    objectStorageContainer.getMappedPort(S3_PORT)
                )
            );
            registry.add("storage.access-key", () -> "test-access-key");
            registry.add("storage.secret-key", () -> "test-secret-key");
            registry.add("storage.bucket", () -> TEST_BUCKET);
        };
    }
}
