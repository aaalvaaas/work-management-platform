package ru.itmo.wmp.mail.infrastructure.imap;

import jakarta.mail.*;
import jakarta.mail.search.FlagTerm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.itmo.wmp.mail.application.mailbox.MailboxConnection;
import ru.itmo.wmp.mail.exception.MailImportException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

@Slf4j
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
                .map(this::parseOrSkip)
                .filter(Objects::nonNull)
                .toList();
        } catch (MessagingException e) {
            throw new MailImportException("Failed to fetch unread mails", e);
        }

    }

    private ParsedMail parseOrSkip(Message message) {
        try {
            return mailParser.parse(message);
        } catch (MessagingException | IOException | RuntimeException exception) {
            log.warn("Mail is skipped, it cannot be parsed: {}", exception.getMessage());

            return null;
        }
    }

    static Properties sessionProperties(MailboxConnection mailbox) {
        String protocol = protocolOf(mailbox);

        Properties properties = new Properties();

        properties.put("mail.store.protocol", protocol);
        properties.put("mail." + protocol + ".host", mailbox.host());
        properties.put("mail." + protocol + ".port", String.valueOf(mailbox.port()));
        properties.put("mail." + protocol + ".ssl.enable", String.valueOf(mailbox.useSsl()));

        return properties;
    }

    private static String protocolOf(MailboxConnection mailbox) {
        return mailbox.useSsl() ? "imaps" : "imap";
    }

    private Store connect(MailboxConnection mailbox) throws MessagingException {
        Session session = Session.getInstance(sessionProperties(mailbox));
        Store store = session.getStore(protocolOf(mailbox));

        store.connect(
            mailbox.host(),
            mailbox.port(),
            mailbox.username(),
            mailbox.password()
        );

        return store;
    }
}
