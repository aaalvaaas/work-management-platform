package ru.itmo.wmp.mail.infrastructure.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.testcontainers.kafka.KafkaContainer;
import ru.itmo.wmp.mail.IntegrationTest;
import ru.itmo.wmp.mail.application.MailProcessor;
import ru.itmo.wmp.mail.application.MailService;
import ru.itmo.wmp.mail.domain.MailMessage;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@IntegrationTest
public class MailEventPublishingIntegrationTest {
    @Autowired
    private MailProcessor mailProcessor;

    @Autowired
    private MailService mailService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaContainer kafkaContainer;

    @Value("${wmp.events.mail-parsed-topic}")
    private String topic;

    @Test
    void shouldPublishTheParsedMailToKafka() throws Exception {
        String messageId = "kafka-" + UUID.randomUUID();

        try (KafkaConsumer<String, String> consumer = consumer()) {
            consumer.subscribe(List.of(topic));
            consumer.poll(Duration.ofSeconds(1));

            mailProcessor.process(mailService.receiveMail(mail(messageId)));

            ConsumerRecord<String, String> record = awaitRecord(consumer, messageId);

            assertNotNull(record, "no event arrived in " + topic);

            JsonNode event = objectMapper.readTree(record.value());

            assertEquals(messageId, event.get("messageId").asText());
            assertEquals(10, event.get("organizationId").asInt());
            assertEquals("закупка ноутбуков", event.get("title").asText());
            assertEquals("Нужны три ноутбука", event.get("description").asText());
            assertEquals("ivanov@niuitmo.ru", event.get("requesterEmail").asText());
            assertTrue(event.get("requiresApproval").asBoolean());
        }
    }

    private ConsumerRecord<String, String> awaitRecord(
        KafkaConsumer<String, String> consumer,
        String messageId
    ) {
        long deadline = System.currentTimeMillis() + 20_000;

        while (System.currentTimeMillis() < deadline) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));

            for (ConsumerRecord<String, String> record : records) {
                if (messageId.equals(record.key())) {
                    return record;
                }
            }
        }

        return null;
    }

    private KafkaConsumer<String, String> consumer() {
        return new KafkaConsumer<>(Map.of(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers(),
            ConsumerConfig.GROUP_ID_CONFIG, "mail-event-test-" + UUID.randomUUID(),
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class
        ));
    }

    private MailMessage mail(String messageId) {
        MailMessage mail = new MailMessage();

        mail.setMessageId(messageId);
        mail.setOrganizationId(10L);
        mail.setMailboxId(1L);
        mail.setSenderEmail("ivanov@niuitmo.ru");
        mail.setRecipientEmail("megafaculty@niuitmo.ru");
        mail.setSubject("СОГЛАСОВАНИЕ: закупка ноутбуков");
        mail.setPlainTextBody("Нужны три ноутбука");

        return mail;
    }
}
