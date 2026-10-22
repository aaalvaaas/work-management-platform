package ru.itmo.wmp.mail.infrastructure.mongo;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Document(collection = "mail_documents")
public class MailDocument {
    @Id
    private String messageId;

    @Indexed
    private Long organizationId;

    private Long mailboxId;
    private String senderEmail;
    private String recipientEmail;
    private String subject;
    private LocalDateTime receivedAt;
    private Map<String, List<String>> headers;
    private String textBody;
    private String htmlBody;
    private List<Attachment> attachments;
    private LocalDateTime storedAt;

    @Getter
    @Setter
    public static class Attachment {
        private String filename;
        private String contentType;
        private long size;
        private String storageKey;
    }
}
