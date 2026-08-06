package ru.itmo.wmp.mail.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.itmo.wmp.mail.domain.MailMessage;
import ru.itmo.wmp.mail.domain.MailProcessingStatus;
import ru.itmo.wmp.mail.domain.MailRepository;
import ru.itmo.wmp.mail.domain.MailStatusTransition;
import ru.itmo.wmp.mail.exception.DuplicateMailException;
import ru.itmo.wmp.mail.exception.InvalidMailStatusTransitionException;
import ru.itmo.wmp.mail.exception.MailNotFoundException;

import java.time.LocalDateTime;
import java.util.List;

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
        return changeStatus(messageId, MailProcessingStatus.PROCESSING);
    }

    public MailMessage markAsProcessed(String messageId) {
        return changeStatus(messageId, MailProcessingStatus.PROCESSED);
    }

    public MailMessage markAsFailed(String messageId) {
        return changeStatus(messageId, MailProcessingStatus.FAILED);
    }

    public MailMessage getMail(String messageId) {
        return getByMessageId(messageId);
    }

    public List<MailMessage> getAll() {
        return mailRepository.findAll();
    }

    private MailMessage getByMessageId(String messageId) {
        return mailRepository.findByMessageId(messageId)
            .orElseThrow(() -> new MailNotFoundException(messageId));
    }

    private MailMessage changeStatus(String messageId, MailProcessingStatus newStatus) {
        MailMessage mail = getByMessageId(messageId);

        if (!MailStatusTransition.canMove(mail.getProcessingStatus(), newStatus)) {
            throw new InvalidMailStatusTransitionException(mail.getProcessingStatus(), newStatus);
        }

        mail.setProcessingStatus(newStatus);

        if (newStatus == MailProcessingStatus.PROCESSED || newStatus == MailProcessingStatus.FAILED) {
            mail.setProcessedAt(LocalDateTime.now());
        }

        return mailRepository.save(mail);
    }
}
