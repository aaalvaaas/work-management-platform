package ru.itmo.wmp.mail.api.mapper;

import org.springframework.stereotype.Component;
import ru.itmo.wmp.mail.domain.MailAttachment;
import ru.itmo.wmp.mail.dto.response.MailAttachmentResponse;

@Component
public class MailAttachmentMapper {
    public MailAttachmentResponse toResponse(MailAttachment attachment) {
        return new MailAttachmentResponse(
            attachment.getId(),
            attachment.getFilename(),
            attachment.getContentType(),
            attachment.getSize()
        );
    }
}
