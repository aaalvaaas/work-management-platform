package ru.itmo.wmp.tasks.infrastructure.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.kafka.KafkaContainer;
import ru.itmo.wmp.tasks.IntegrationTestContainers;
import ru.itmo.wmp.tasks.application.repository.TaskRepository;
import ru.itmo.wmp.tasks.domain.Task;
import ru.itmo.wmp.tasks.domain.TaskSource;
import ru.itmo.wmp.tasks.domain.TaskStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(IntegrationTestContainers.class)
public class MailTaskIntegrationTest {
    private static final long ORGANIZATION_ID = 4100L;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private KafkaContainer kafkaContainer;

    @Value("${wmp.events.mail-parsed-topic}")
    private String topic;

    @Test
    void shouldCreateOneTaskEvenWhenTheEventArrivesTwice() {
        String messageId = "mail-" + UUID.randomUUID();

        send(messageId);
        send(messageId);

        await().atMost(30, TimeUnit.SECONDS)
            .until(() -> taskOf(messageId).isPresent());

        Task task = taskOf(messageId).orElseThrow();

        assertEquals(ORGANIZATION_ID, task.getOrganizationId());
        assertEquals("Закупка ноутбуков", task.getTitle());
        assertEquals(TaskStatus.NEW, task.getStatus());
        assertEquals(TaskSource.MAIL, task.getSource());
        assertEquals("ivanov@niuitmo.ru", task.getRequesterEmail());
        assertTrue(task.isRequiresApproval());
        assertNull(task.getAuthorUserId());

        List<Task> tasks = taskRepository.findByOrganizationIdOrderByIdDesc(ORGANIZATION_ID);

        assertEquals(1, tasks.size(), "the same mail must not become two tasks");
    }

    private Optional<Task> taskOf(String messageId) {
        return taskRepository.findBySourceAndSourceReference(TaskSource.MAIL, messageId);
    }

    private void send(String messageId) {
        String payload = """
            {
              "messageId": "%s",
              "organizationId": %d,
              "mailboxId": 1,
              "requesterEmail": "ivanov@niuitmo.ru",
              "subject": "СОГЛАСОВАНИЕ: закупка ноутбуков",
              "receivedAt": "2026-10-17T10:00:00",
              "title": "Закупка ноутбуков",
              "description": "Нужны три ноутбука",
              "requiresApproval": true
            }
            """.formatted(messageId, ORGANIZATION_ID);

        try (KafkaProducer<String, String> producer = producer()) {
            producer.send(new ProducerRecord<>(topic, messageId, payload));
            producer.flush();
        }
    }

    private KafkaProducer<String, String> producer() {
        return new KafkaProducer<>(Map.of(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers(),
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class
        ));
    }
}
