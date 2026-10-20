package ru.itmo.wmp.tasks.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaConnectionDetails;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;
import ru.itmo.wmp.tasks.application.mail.ParsedMailEvent;

import java.util.Map;

@Configuration
public class KafkaConfig {
    private static final long RETRY_INTERVAL = 2000L;
    private static final long RETRY_ATTEMPTS = 3L;

    @Bean
    public ConsumerFactory<String, ParsedMailEvent> mailEventConsumerFactory(
        KafkaProperties kafkaProperties,
        KafkaConnectionDetails connectionDetails,
        ObjectMapper objectMapper
    ) {
        Map<String, Object> settings = kafkaProperties.buildConsumerProperties(null);

        settings.put(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
            connectionDetails.getConsumerBootstrapServers()
        );

        JsonDeserializer<ParsedMailEvent> valueDeserializer =
            new JsonDeserializer<>(ParsedMailEvent.class, objectMapper, false);

        valueDeserializer.setUseTypeHeaders(false);

        return new DefaultKafkaConsumerFactory<>(
            settings,
            new StringDeserializer(),
            new ErrorHandlingDeserializer<>(valueDeserializer)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ParsedMailEvent>
    mailEventListenerContainerFactory(
        ConsumerFactory<String, ParsedMailEvent> mailEventConsumerFactory,
        KafkaTemplate<String, Object> deadLetterKafkaTemplate
    ) {
        ConcurrentKafkaListenerContainerFactory<String, ParsedMailEvent> factory =
            new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(mailEventConsumerFactory);

        factory.setCommonErrorHandler(new DefaultErrorHandler(
            new DeadLetterPublishingRecoverer(deadLetterKafkaTemplate),
            new FixedBackOff(RETRY_INTERVAL, RETRY_ATTEMPTS)
        ));

        return factory;
    }

    @Bean
    public KafkaTemplate<String, Object> deadLetterKafkaTemplate(
        KafkaProperties kafkaProperties,
        KafkaConnectionDetails connectionDetails,
        ObjectMapper objectMapper
    ) {
        Map<String, Object> settings = kafkaProperties.buildProducerProperties(null);

        settings.put(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
            connectionDetails.getProducerBootstrapServers()
        );

        JsonSerializer<Object> valueSerializer = new JsonSerializer<>(objectMapper);

        valueSerializer.setAddTypeInfo(false);

        ProducerFactory<String, Object> producerFactory = new DefaultKafkaProducerFactory<>(
            settings,
            new StringSerializer(),
            valueSerializer
        );

        return new KafkaTemplate<>(producerFactory);
    }
}
