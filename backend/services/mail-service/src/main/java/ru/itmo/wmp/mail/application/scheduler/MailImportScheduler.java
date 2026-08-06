package ru.itmo.wmp.mail.application.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.itmo.wmp.mail.application.MailImportService;

@Slf4j
@Component
@RequiredArgsConstructor
public class MailImportScheduler {
    private final MailImportService mailImportService;

    @Scheduled(fixedDelayString = "${mail.import.fixed-delay}")
    public void importUnreadMails() {
        log.debug("Starting scheduled mail import");

        mailImportService.importUnreadMails();
    }
}
