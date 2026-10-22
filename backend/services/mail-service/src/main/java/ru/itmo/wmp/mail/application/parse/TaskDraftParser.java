package ru.itmo.wmp.mail.application.parse;

import ru.itmo.wmp.mail.domain.MailMessage;

public interface TaskDraftParser {
    TaskDraft parse(MailMessage mail);

    String name();

    String version();
}
