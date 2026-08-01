package ru.itmo.wmp.mail.api;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.wmp.mail.api.mapper.MailImportMapper;
import ru.itmo.wmp.mail.application.MailImportResult;
import ru.itmo.wmp.mail.application.MailImportService;
import ru.itmo.wmp.mail.dto.response.MailImportResponse;

@RestController
@RequestMapping("/api/v1/imap")
@RequiredArgsConstructor
public class ImapController {
    private final MailImportService mailImportService;
    private final MailImportMapper mailImportMapper;

    @PostMapping("/import")
    public MailImportResponse importMails() {
        MailImportResult result = mailImportService.importUnreadMails();

        return mailImportMapper.toResponse(result);
    }
}
