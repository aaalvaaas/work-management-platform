package ru.itmo.wmp.mail.application.attachment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.wmp.mail.application.repository.MailAttachmentRepository;
import ru.itmo.wmp.mail.application.storage.StorageService;
import ru.itmo.wmp.mail.domain.MailAttachment;
import ru.itmo.wmp.mail.domain.MailMessage;
import ru.itmo.wmp.mail.exception.MailAttachmentNotFoundException;
import ru.itmo.wmp.mail.infrastructure.imap.ParsedAttachment;

import java.io.InputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MailAttachmentService {
    private final StorageService storageService;
    private final MailAttachmentRepository mailAttachmentRepository;

    public MailAttachment save(MailMessage mailMessage, ParsedAttachment attachment) {
        String storageKey = storageService.upload(
            attachment.filename(),
            attachment.inputStream(),
            attachment.size(),
            attachment.contentType()
        );

        try {
            MailAttachment mailAttachment = new MailAttachment();

            mailAttachment.setMailMessage(mailMessage);
            mailAttachment.setFilename(attachment.filename());
            mailAttachment.setContentType(attachment.contentType());
            mailAttachment.setSize(attachment.size());
            mailAttachment.setStorageKey(storageKey);

            return mailAttachmentRepository.save(mailAttachment);
        } catch (RuntimeException e) {
            storageService.delete(storageKey);
            throw e;
        }
    }

    public List<MailAttachment> findAllByMailMessageId(Long mailMessageId) {
        return mailAttachmentRepository.findAllByMailMessageId(mailMessageId);
    }

    @Transactional(readOnly = true)
    public MailAttachmentDownload download(Long mailMessageId, Long attachmentId) {
        MailAttachment attachment = mailAttachmentRepository
            .findById(attachmentId)
            .orElseThrow(() -> new MailAttachmentNotFoundException(attachmentId));

        if (!attachment.getMailMessage().getId().equals(mailMessageId)) {
            throw new MailAttachmentNotFoundException(attachmentId);
        }

        InputStream inputStream = storageService.download(attachment.getStorageKey());

        return new MailAttachmentDownload(
            attachment.getFilename(),
            attachment.getContentType(),
            attachment.getSize(),
            inputStream
        );
    }
}
