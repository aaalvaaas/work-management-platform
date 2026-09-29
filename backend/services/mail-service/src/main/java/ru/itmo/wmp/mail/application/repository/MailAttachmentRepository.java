package ru.itmo.wmp.mail.application.repository;

import ru.itmo.wmp.mail.domain.MailAttachment;

import java.util.List;

public interface MailAttachmentRepository {
    MailAttachment save (MailAttachment mailAttachment);

    List<MailAttachment> findAllByMailMessageId(Long mailMessageId);
}
