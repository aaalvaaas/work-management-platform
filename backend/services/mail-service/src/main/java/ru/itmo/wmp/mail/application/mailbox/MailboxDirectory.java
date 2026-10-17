package ru.itmo.wmp.mail.application.mailbox;

import java.util.List;

public interface MailboxDirectory {
    List<MailboxConnection> activeMailboxes();

    void reportCheck(Long mailboxId, boolean successful, String error);
}
