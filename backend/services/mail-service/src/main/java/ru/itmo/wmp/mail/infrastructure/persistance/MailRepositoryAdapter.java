package ru.itmo.wmp.mail.infrastructure.persistance;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import ru.itmo.wmp.mail.domain.MailMessage;
import ru.itmo.wmp.mail.domain.MailRepository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MailRepositoryAdapter implements MailRepository {
    private final JpaMailRepository repository;

    @Override
    public MailMessage save(MailMessage mailMessage) {
        return repository.save(mailMessage);
    }

    @Override
    public Optional<MailMessage> findByMessageId(String messageId) {
        return repository.findByMessageId(messageId);
    }

    @Override
    public boolean existsByMessageId(String messageId) {
        return repository.existsByMessageId(messageId);
    }
}
