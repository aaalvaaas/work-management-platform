package ru.itmo.wmp.mail.infrastructure.parse;

import org.springframework.stereotype.Component;
import ru.itmo.wmp.mail.application.parse.TaskDraft;
import ru.itmo.wmp.mail.application.parse.TaskDraftParser;
import ru.itmo.wmp.mail.domain.MailMessage;

import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class RuleBasedTaskDraftParser implements TaskDraftParser {
    private static final String APPROVAL_STEM = "согласован";

    private static final Pattern APPROVAL_PREFIX = Pattern.compile(
        "^\\s*согласовани[ея]\\s*[:\\-–—]\\s*",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final int MAX_TITLE_LENGTH = 255;
    private static final int MAX_DESCRIPTION_LENGTH = 8000;

    @Override
    public TaskDraft parse(MailMessage mail) {
        String subject = mail.getSubject() == null ? "" : mail.getSubject().trim();

        return new TaskDraft(
            title(subject, mail.getSenderEmail()),
            description(mail.getPlainTextBody()),
            requiresApproval(subject),
            mail.getSenderEmail()
        );
    }

    private boolean requiresApproval(String subject) {
        return subject.toLowerCase(Locale.ROOT).contains(APPROVAL_STEM);
    }

    private String title(String subject, String senderEmail) {
        String title = APPROVAL_PREFIX.matcher(subject).replaceFirst("").trim();

        if (title.isBlank()) {
            title = "Письмо от " + senderEmail;
        }

        return cut(title, MAX_TITLE_LENGTH);
    }

    private String description(String body) {
        if (body == null) {
            return "";
        }

        return cut(body.strip(), MAX_DESCRIPTION_LENGTH);
    }

    private String cut(String value, int limit) {
        return value.length() <= limit ? value : value.substring(0, limit);
    }
}
