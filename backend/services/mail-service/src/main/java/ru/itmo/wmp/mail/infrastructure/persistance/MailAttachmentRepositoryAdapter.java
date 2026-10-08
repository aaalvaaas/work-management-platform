package ru.itmo.wmp.mail.infrastructure.persistance;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import ru.itmo.wmp.mail.application.repository.MailAttachmentRepository;
import ru.itmo.wmp.mail.domain.MailAttachment;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MailAttachmentRepositoryAdapter implements MailAttachmentRepository {
    private final JpaMailAttachmentRepository repository;

    @Override
    public MailAttachment save(MailAttachment mailAttachment) {
        return repository.save(mailAttachment);
    }

    @Override
    public List<MailAttachment> findAllByMailMessageId(Long mailMessageId) {
        return repository.findAllByMailMessageId(mailMessageId);
    }

    @Override
    public Optional<MailAttachment> findById(Long id) {
        return repository.findById(id);
    }
}
