package ru.itmo.wmp.mail.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.itmo.wmp.mail.domain.MailMessage;
import ru.itmo.wmp.mail.domain.MailProcessingStatus;
import ru.itmo.wmp.mail.domain.MailRepository;
import ru.itmo.wmp.mail.exception.DuplicateMailException;
import ru.itmo.wmp.mail.exception.MailNotFoundException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MailService {
    private final MailRepository mailRepository;

    public MailMessage receiveMail(MailMessage mailMessage) {
        if (mailRepository.existsByMessageId(mailMessage.getMessageId())) {
            throw new DuplicateMailException(mailMessage.getMessageId());
        }

        mailMessage.setReceivedAt(LocalDateTime.now());
        mailMessage.setProcessingStatus(MailProcessingStatus.RECEIVED);

        return mailRepository.save(mailMessage);
    }

    public MailMessage markAsProcessing(String messageId) {
        MailMessage mail = getByMessageId(messageId);

        mail.setProcessingStatus(MailProcessingStatus.PROCESSING);

        return mailRepository.save(mail);
    }

    public MailMessage markAsProcessed(String messageId) {
        MailMessage mail = getByMessageId(messageId);

        mail.setProcessingStatus(MailProcessingStatus.PROCESSED);
        mail.setProcessedAt(LocalDateTime.now());

        return mailRepository.save(mail);
    }

    public MailMessage markAsFailed(String messageId) {
        MailMessage mail = getByMessageId(messageId);

        mail.setProcessingStatus(MailProcessingStatus.FAILED);
        mail.setProcessedAt(LocalDateTime.now());

        return mailRepository.save(mail);
    }

    private MailMessage getByMessageId(String messageId) {
        return mailRepository.findByMessageId(messageId)
            .orElseThrow(() -> new MailNotFoundException(messageId));
    }
}
