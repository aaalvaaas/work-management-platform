package ru.itmo.wmp.mail.application.repository;

import ru.itmo.wmp.mail.domain.MailMessage;

import java.util.List;
import java.util.Optional;

public interface MailRepository {
    MailMessage save(MailMessage mailMessage);

    Optional<MailMessage> findByMessageId(String messageId);

    boolean existsByMessageId(String messageId);

    List<MailMessage> findAll();

    void deleteAll();
}
