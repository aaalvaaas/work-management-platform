package ru.itmo.wmp.mail.infrastructure.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.itmo.wmp.mail.application.event.MailEventPublisher;
import ru.itmo.wmp.mail.application.event.MailParsedEvent;
import ru.itmo.wmp.mail.config.properties.MailEventProperties;

@Slf4j
@Component
public class KafkaMailEventPublisher implements MailEventPublisher {
    private final KafkaTemplate<String, MailParsedEvent> kafkaTemplate;
    private final MailEventProperties properties;

    public KafkaMailEventPublisher(
        KafkaTemplate<String, MailParsedEvent> mailEventKafkaTemplate,
        MailEventProperties properties
    ) {
        this.kafkaTemplate = mailEventKafkaTemplate;
        this.properties = properties;
    }

    @Override
    public void publishParsed(MailParsedEvent event) {
        kafkaTemplate.send(properties.mailParsedTopic(), event.messageId(), event).join();

        log.info(
            "Published mail {} of organization {} as a parsed event",
            event.messageId(),
            event.organizationId()
        );
    }
}
