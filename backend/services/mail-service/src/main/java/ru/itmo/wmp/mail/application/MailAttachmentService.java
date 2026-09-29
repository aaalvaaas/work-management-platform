package ru.itmo.wmp.mail.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.itmo.wmp.mail.application.repository.MailAttachmentRepository;
import ru.itmo.wmp.mail.application.storage.StorageService;
import ru.itmo.wmp.mail.domain.MailAttachment;
import ru.itmo.wmp.mail.domain.MailMessage;
import ru.itmo.wmp.mail.infrastructure.imap.ParsedAttachment;

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
}
