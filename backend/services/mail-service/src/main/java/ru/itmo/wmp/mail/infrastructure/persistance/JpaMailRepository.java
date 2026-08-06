package ru.itmo.wmp.mail.infrastructure.persistance;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.wmp.mail.domain.MailMessage;

import java.util.Optional;

public interface JpaMailRepository extends JpaRepository<MailMessage, Long> {
    Optional<MailMessage> findByMessageId(String messageId);

    boolean existsByMessageId(String messageId);
}
