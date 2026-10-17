package ru.itmo.wmp.mail.application.mailbox;

import java.util.List;

public interface MailboxDirectory {
    List<MailboxConnection> activeMailboxes();
}
