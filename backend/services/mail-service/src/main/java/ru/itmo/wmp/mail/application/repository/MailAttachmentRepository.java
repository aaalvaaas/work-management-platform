package ru.itmo.wmp.mail.application.repository;

import ru.itmo.wmp.mail.domain.MailAttachment;

import java.util.List;
import java.util.Optional;

public interface MailAttachmentRepository {
    MailAttachment save (MailAttachment mailAttachment);

    List<MailAttachment> findAllByMailMessageId(Long mailMessageId);

    Optional<MailAttachment> findById(Long id);
}
