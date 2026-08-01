package ru.itmo.wmp.mail.infrastructure.imap;

import jakarta.mail.Session;
import jakarta.mail.Store;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.itmo.wmp.mail.config.properties.MailProperties;

import java.util.Properties;

@Component
@RequiredArgsConstructor
public class ImapMailClient {
    private final MailProperties properties;

    private Session createSession() {
        Properties props = new Properties();

        props.put("mail.store.protocol", "imaps");
        props.put("mail.imap.host", properties.host());
        props.put("mail.imap.port", properties.port());
        props.put("mail.imap.ssl.enabled", properties.ssl());

        return Session.getInstance(props);
    }

    private Store connect() throws Exception {
        Session session = createSession();
        Store store = session.getStore();

        store.connect(
            properties.host(),
            properties.username(),
            properties.password()
        );

        return store;
    }
}
