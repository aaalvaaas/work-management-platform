package ru.itmo.wmp.mail.infrastructure.mongo;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@Document(collection = "mail_parse_results")
public class MailParseDocument {
    @Id
    private String id;

    @Indexed
    private String messageId;

    private Long organizationId;
    private String parser;
    private String parserVersion;
    private LocalDateTime parsedAt;
    private String title;
    private String description;
    private boolean requiresApproval;
    private String requesterEmail;
    private Double confidence;
    private Map<String, Object> details;
}
