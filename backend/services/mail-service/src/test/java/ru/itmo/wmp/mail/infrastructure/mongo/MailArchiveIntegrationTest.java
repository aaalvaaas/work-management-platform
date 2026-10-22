package ru.itmo.wmp.mail.infrastructure.mongo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.itmo.wmp.mail.IntegrationTest;
import ru.itmo.wmp.mail.application.archive.ArchivedAttachment;
import ru.itmo.wmp.mail.application.archive.ArchivedMail;
import ru.itmo.wmp.mail.application.archive.MailArchive;
import ru.itmo.wmp.mail.application.archive.MailParseArchive;
import ru.itmo.wmp.mail.application.archive.ParseRecord;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@IntegrationTest
public class MailArchiveIntegrationTest {
    @Autowired
    private MailArchive mailArchive;

    @Autowired
    private MailParseArchive mailParseArchive;

    @Test
    void shouldKeepTheLetterWithHeadersAndAttachments() {
        String messageId = messageId();

        mailArchive.store(mail(messageId));

        ArchivedMail stored = mailArchive.find(messageId).orElseThrow();

        assertEquals(10L, stored.organizationId());
        assertEquals("ivanov@niuitmo.ru", stored.senderEmail());
        assertEquals("СОГЛАСОВАНИЕ: закупка", stored.subject());
        assertEquals("Нужны три ноутбука", stored.textBody());
        assertEquals("<p>Нужны три ноутбука</p>", stored.htmlBody());
        assertEquals(List.of("Microsoft Outlook 16.0"), stored.headers().get("x-mailer"));
        assertEquals(1, stored.attachments().size());
        assertEquals("смета.pdf", stored.attachments().getFirst().filename());
    }

    @Test
    void shouldAnswerNothingForALetterThatWasNeverStored() {
        assertTrue(mailArchive.find(messageId()).isEmpty());
    }

    @Test
    void shouldKeepEveryParseOfTheSameLetterNewestFirst() {
        String messageId = messageId();

        mailParseArchive.record(parse(messageId, "RULES", "v1", LocalDateTime.of(2026, 10, 17, 10, 0)));
        mailParseArchive.record(parse(messageId, "AI", "v1", LocalDateTime.of(2026, 10, 17, 12, 0)));

        List<ParseRecord> parses = mailParseArchive.parsesOf(messageId);

        assertEquals(2, parses.size());
        assertEquals("AI", parses.getFirst().parser());
        assertEquals("RULES", parses.getLast().parser());
        assertEquals("Закупка", parses.getFirst().title());
        assertTrue(parses.getFirst().requiresApproval());
        assertEquals("СОГЛАСОВАНИЕ: закупка", parses.getFirst().details().get("subject"));
    }

    @Test
    void shouldReplaceTheLetterWhenItIsStoredAgain() {
        String messageId = messageId();

        mailArchive.store(mail(messageId));
        mailArchive.store(mail(messageId));

        assertEquals("Нужны три ноутбука", mailArchive.find(messageId).orElseThrow().textBody());
    }

    private String messageId() {
        return "archive-" + UUID.randomUUID();
    }

    private ArchivedMail mail(String messageId) {
        return new ArchivedMail(
            messageId,
            10L,
            1L,
            "ivanov@niuitmo.ru",
            "megafaculty@niuitmo.ru",
            "СОГЛАСОВАНИЕ: закупка",
            LocalDateTime.of(2026, 10, 17, 10, 0),
            Map.of(
                "x-mailer", List.of("Microsoft Outlook 16.0"),
                "message-id", List.of(messageId)
            ),
            "Нужны три ноутбука",
            "<p>Нужны три ноутбука</p>",
            List.of(new ArchivedAttachment("смета.pdf", "application/pdf", 1024L, "mail/смета.pdf"))
        );
    }

    private ParseRecord parse(
        String messageId,
        String parser,
        String version,
        LocalDateTime parsedAt
    ) {
        return new ParseRecord(
            messageId,
            10L,
            parser,
            version,
            parsedAt,
            "Закупка",
            "Нужны три ноутбука",
            true,
            "ivanov@niuitmo.ru",
            null,
            Map.of("subject", "СОГЛАСОВАНИЕ: закупка")
        );
    }
}
