package ru.itmo.wmp.mail.infrastructure.persistance;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.wmp.mail.domain.MailAttachment;

import java.util.List;

public interface JpaMailAttachmentRepository extends JpaRepository<MailAttachment, Long> {
    List<MailAttachment> findAllByMailMessageId(Long mailMessageId);
}
