package ru.itmo.wmp.mail.infrastructure.imap;

import jakarta.mail.*;
import jakarta.mail.search.FlagTerm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.itmo.wmp.mail.application.mailbox.MailboxConnection;
import ru.itmo.wmp.mail.exception.MailImportException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

@Component
@RequiredArgsConstructor
public class ImapMailClient {
    private final MailParser mailParser;

    private List<Message> getUnreadMessages(MailboxConnection mailbox) throws MessagingException {
        Store store = connect(mailbox);
        Folder inbox = store.getFolder(mailbox.folder());
        inbox.open(Folder.READ_WRITE);

        Message[] messages = inbox.search(
            new FlagTerm(
                new Flags(Flags.Flag.SEEN),
                false
            )
        );

        return Arrays.asList(messages);
    }

    public void markAsRead(Message message) {
        try {
            message.setFlag(
                Flags.Flag.SEEN,
                true
            );
        } catch (MessagingException e) {
            throw new MailImportException("Failed to mark mail as read", e);
        }
    }

    public List<ParsedMail> fetchUnread(MailboxConnection mailbox) {
        try {
            List<Message> messages = getUnreadMessages(mailbox);

            return messages.stream()
                .map(message -> {
                    try {
                        return mailParser.parse(message);
                    } catch (MessagingException | IOException e) {
                        throw new MailImportException("Failed to parse mail", e);
                    }
                })
                .toList();
        } catch (MessagingException e) {
            throw new MailImportException("Failed to fetch unread mails", e);
        }

    }

    private Session createSession(MailboxConnection mailbox) {
        Properties props = new Properties();

        props.put("mail.store.protocol", "imaps");
        props.put("mail.imap.host", mailbox.host());
        props.put("mail.imap.port", mailbox.port());
        props.put("mail.imap.ssl.enabled", mailbox.useSsl());

        return Session.getInstance(props);
    }

    private Store connect(MailboxConnection mailbox) throws MessagingException {
        Session session = createSession(mailbox);
        Store store = session.getStore();

        store.connect(
            mailbox.host(),
            mailbox.port(),
            mailbox.username(),
            mailbox.password()
        );

        return store;
    }
}
