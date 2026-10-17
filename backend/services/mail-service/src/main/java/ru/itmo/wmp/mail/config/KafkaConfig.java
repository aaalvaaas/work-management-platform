package ru.itmo.wmp.mail.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.apache.kafka.clients.admin.NewTopic;
import ru.itmo.wmp.mail.application.event.MailParsedEvent;
import ru.itmo.wmp.mail.config.properties.MailEventProperties;

import java.util.Map;

@Configuration
public class KafkaConfig {
    @Bean
    public ProducerFactory<String, MailParsedEvent> mailEventProducerFactory(
        KafkaProperties kafkaProperties,
        ObjectMapper objectMapper
    ) {
        Map<String, Object> settings = kafkaProperties.buildProducerProperties(null);

        settings.put(ProducerConfig.ACKS_CONFIG, "all");
        settings.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        JsonSerializer<MailParsedEvent> valueSerializer = new JsonSerializer<>(objectMapper);

        valueSerializer.setAddTypeInfo(false);

        return new DefaultKafkaProducerFactory<>(
            settings,
            new StringSerializer(),
            valueSerializer
        );
    }

    @Bean
    public KafkaTemplate<String, MailParsedEvent> mailEventKafkaTemplate(
        ProducerFactory<String, MailParsedEvent> mailEventProducerFactory
    ) {
        return new KafkaTemplate<>(mailEventProducerFactory);
    }

    @Bean
    public NewTopic mailParsedTopic(MailEventProperties properties) {
        return TopicBuilder.name(properties.mailParsedTopic())
            .partitions(properties.partitions())
            .replicas(1)
            .build();
    }
}
