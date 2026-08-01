package ru.itmo.wmp.mail.infrastructure.imap;

import jakarta.mail.*;
import jakarta.mail.search.FlagTerm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.itmo.wmp.mail.config.properties.MailProperties;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

@Component
@RequiredArgsConstructor
public class ImapMailClient {
    private final MailProperties properties;

    public List<Message> getUnreadMessages() throws MessagingException {
        Store store = connect();
        Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_ONLY);

        Message[] messages = inbox.search(
            new FlagTerm(
                new Flags(Flags.Flag.SEEN),
                false
            )
        );

        return Arrays.asList(messages);
    }

    private Session createSession() {
        Properties props = new Properties();

        props.put("mail.store.protocol", "imaps");
        props.put("mail.imap.host", properties.host());
        props.put("mail.imap.port", properties.port());
        props.put("mail.imap.ssl.enabled", properties.ssl());

        return Session.getInstance(props);
    }

    private Store connect() throws MessagingException {
        Session session = createSession();
        Store store = session.getStore();

        store.connect(
            properties.host(),
            properties.port(),
            properties.username(),
            properties.password()
        );

        return store;
    }
}
