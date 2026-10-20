package ru.itmo.wmp.mail.application.event;

public interface MailEventPublisher {
    void publishParsed(MailParsedEvent event);
}
