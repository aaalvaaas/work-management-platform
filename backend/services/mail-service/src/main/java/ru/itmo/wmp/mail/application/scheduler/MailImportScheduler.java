package ru.itmo.wmp.mail.application.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.itmo.wmp.mail.application.MailImportService;

@Component
@RequiredArgsConstructor
public class MailImportScheduler {
    private final MailImportService mailImportService;

    @Scheduled(fixedDelayString = "${mail.import.fixed-delay}")
    public void importUnreadMails() {
        mailImportService.importUnreadMails();
    }
}
