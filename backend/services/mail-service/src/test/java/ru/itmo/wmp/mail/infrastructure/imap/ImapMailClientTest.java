package ru.itmo.wmp.mail.infrastructure.imap;

import org.junit.jupiter.api.Test;
import ru.itmo.wmp.mail.application.mailbox.MailboxConnection;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class ImapMailClientTest {
    @Test
    void shouldUseSecureProtocolForAMailboxWithSsl() {
        Properties properties = ImapMailClient.sessionProperties(mailbox(true));

        assertEquals("imaps", properties.get("mail.store.protocol"));
        assertEquals("imap.mail.ru", properties.get("mail.imaps.host"));
        assertEquals("993", properties.get("mail.imaps.port"));
        assertEquals("true", properties.get("mail.imaps.ssl.enable"));
    }

    @Test
    void shouldUsePlainProtocolForAMailboxWithoutSsl() {
        Properties properties = ImapMailClient.sessionProperties(mailbox(false));

        assertEquals("imap", properties.get("mail.store.protocol"));
        assertEquals("imap.mail.ru", properties.get("mail.imap.host"));
        assertEquals("143", properties.get("mail.imap.port"));
        assertEquals("false", properties.get("mail.imap.ssl.enable"));
    }

    private MailboxConnection mailbox(boolean useSsl) {
        return new MailboxConnection(
            1L,
            10L,
            "megafaculty",
            "imap.mail.ru",
            useSsl ? 993 : 143,
            "megafaculty@mail.ru",
            "secret",
            "INBOX",
            useSsl
        );
    }
}
