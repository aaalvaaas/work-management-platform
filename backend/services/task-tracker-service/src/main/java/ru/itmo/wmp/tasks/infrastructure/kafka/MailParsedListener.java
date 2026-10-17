package ru.itmo.wmp.tasks.infrastructure.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.itmo.wmp.tasks.application.mail.MailTaskService;
import ru.itmo.wmp.tasks.application.mail.ParsedMailEvent;

@Component
@RequiredArgsConstructor
public class MailParsedListener {
    private final MailTaskService mailTaskService;

    @KafkaListener(
        topics = "${wmp.events.mail-parsed-topic}",
        groupId = "${wmp.events.group-id}",
        containerFactory = "mailEventListenerContainerFactory"
    )
    public void onMailParsed(ParsedMailEvent event) {
        mailTaskService.createFromMail(event).ifPresent(mailTaskService::project);
    }
}
