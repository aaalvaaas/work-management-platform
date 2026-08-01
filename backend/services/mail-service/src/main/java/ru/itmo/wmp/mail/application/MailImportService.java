package ru.itmo.wmp.mail.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.itmo.wmp.mail.domain.MailMessage;
import ru.itmo.wmp.mail.exception.DuplicateMailException;
import ru.itmo.wmp.mail.infrastructure.imap.ImapMailClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MailImportService {
    private final ImapMailClient imapMailClient;
    private final MailService mailService;

    public void importUnreadMails() {
        List<MailMessage> mails = imapMailClient.fetchUnread();

        mails.forEach(mail -> {
            try {
                mailService.receiveMail(mail);
            } catch (DuplicateMailException ignored) {}
        });
    }
}
