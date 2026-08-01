package ru.itmo.wmp.mail.api;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.wmp.mail.application.MailImportService;

@RestController
@RequestMapping("/api/v1/imap")
@RequiredArgsConstructor
public class ImapController {
    private final MailImportService mailImportService;

    @PostMapping("/import")
    public void importMails() {
        mailImportService.importUnreadMails();
    }
}
