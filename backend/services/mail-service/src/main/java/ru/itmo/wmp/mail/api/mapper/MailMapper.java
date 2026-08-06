package ru.itmo.wmp.mail.api.mapper;

import org.springframework.stereotype.Component;
import ru.itmo.wmp.mail.domain.MailMessage;
import ru.itmo.wmp.mail.dto.request.MailRequest;
import ru.itmo.wmp.mail.dto.response.MailResponse;

@Component
public class MailMapper {
    public MailMessage toEntity(MailRequest request) {
        MailMessage mail = new MailMessage();

        mail.setMessageId(request.messageId());
        mail.setSenderEmail(request.senderEmail());
        mail.setRecipientEmail(request.recipientEmail());
        mail.setSubject(request.subject());
        mail.setPlainTextBody(request.plainTextBody());

        return mail;
    }

    public MailResponse toResponse(MailMessage mail) {
        return new MailResponse(
            mail.getMessageId(),
            mail.getSenderEmail(),
            mail.getRecipientEmail(),
            mail.getSubject(),
            mail.getPlainTextBody(),
            mail.getProcessingStatus(),
            mail.getReceivedAt(),
            mail.getProcessedAt()
        );
    }
}
