package ru.itmo.wmp.mail.application.archive;

import java.util.List;

public interface MailParseArchive {
    void record(ParseRecord parseRecord);

    List<ParseRecord> parsesOf(String messageId);
}
