package ru.itmo.wmp.mail.application.archive;

import java.util.Optional;

public interface MailArchive {
    void store(ArchivedMail mail);

    Optional<ArchivedMail> find(String messageId);
}
