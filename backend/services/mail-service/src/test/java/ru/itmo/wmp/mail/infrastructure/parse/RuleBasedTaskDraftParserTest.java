package ru.itmo.wmp.mail.infrastructure.parse;

import org.junit.jupiter.api.Test;
import ru.itmo.wmp.mail.application.parse.TaskDraft;
import ru.itmo.wmp.mail.domain.MailMessage;

import static org.junit.jupiter.api.Assertions.*;

public class RuleBasedTaskDraftParserTest {
    private final RuleBasedTaskDraftParser parser = new RuleBasedTaskDraftParser();

    @Test
    void shouldTakeTitleFromSubjectAndDescriptionFromBody() {
        TaskDraft draft = parser.parse(mail("Закупка оборудования", "Нужны три ноутбука"));

        assertEquals("Закупка оборудования", draft.title());
        assertEquals("Нужны три ноутбука", draft.description());
        assertEquals("ivanov@niuitmo.ru", draft.requesterEmail());
        assertFalse(draft.requiresApproval());
    }

    @Test
    void shouldMarkApprovalWhenSubjectMentionsIt() {
        assertTrue(parser.parse(mail("СОГЛАСОВАНИЕ: закупка", "")).requiresApproval());
        assertTrue(parser.parse(mail("Прошу согласования закупки", "")).requiresApproval());
        assertTrue(parser.parse(mail("закупка на согласование", "")).requiresApproval());
        assertFalse(parser.parse(mail("Закупка оборудования", "")).requiresApproval());
    }

    @Test
    void shouldDropTheApprovalPrefixFromTitle() {
        assertEquals("закупка", parser.parse(mail("СОГЛАСОВАНИЕ: закупка", "")).title());
        assertEquals("закупка", parser.parse(mail("Согласование — закупка", "")).title());
        assertEquals(
            "Прошу согласования закупки",
            parser.parse(mail("Прошу согласования закупки", "")).title()
        );
    }

    @Test
    void shouldFallBackToSenderWhenSubjectIsEmpty() {
        assertEquals("Письмо от ivanov@niuitmo.ru", parser.parse(mail("  ", "текст")).title());
    }

    @Test
    void shouldCutAnOverlongTitle() {
        String subject = "а".repeat(300);

        assertEquals(255, parser.parse(mail(subject, "")).title().length());
    }

    private MailMessage mail(String subject, String body) {
        MailMessage mail = new MailMessage();

        mail.setMessageId("test-001");
        mail.setSenderEmail("ivanov@niuitmo.ru");
        mail.setRecipientEmail("megafaculty@niuitmo.ru");
        mail.setSubject(subject);
        mail.setPlainTextBody(body);

        return mail;
    }
}
