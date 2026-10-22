package ru.itmo.wmp.mail.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.itmo.wmp.mail.application.archive.MailParseArchive;
import ru.itmo.wmp.mail.application.archive.ParseRecord;
import ru.itmo.wmp.mail.application.event.MailEventPublisher;
import ru.itmo.wmp.mail.application.event.MailParsedEvent;
import ru.itmo.wmp.mail.application.parse.TaskDraft;
import ru.itmo.wmp.mail.application.parse.TaskDraftParser;
import ru.itmo.wmp.mail.domain.MailMessage;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailProcessor {
    private final MailService mailService;
    private final TaskDraftParser taskDraftParser;
    private final MailEventPublisher mailEventPublisher;
    private final MailParseArchive mailParseArchive;

    public void process(MailMessage mail) {
        String messageId = mail.getMessageId();

        mailService.markAsProcessing(messageId);

        try {
            TaskDraft draft = taskDraftParser.parse(mail);

            record(mail, draft);

            mailEventPublisher.publishParsed(event(mail, draft));

            mailService.markAsProcessed(messageId);
        } catch (Exception e) {
            mailService.markAsFailed(messageId);

            throw e;
        }
    }

    private void record(MailMessage mail, TaskDraft draft) {
        try {
            mailParseArchive.record(new ParseRecord(
                mail.getMessageId(),
                mail.getOrganizationId(),
                taskDraftParser.name(),
                taskDraftParser.version(),
                LocalDateTime.now(),
                draft.title(),
                draft.description(),
                draft.requiresApproval(),
                draft.requesterEmail(),
                null,
                Map.of("subject", mail.getSubject() == null ? "" : mail.getSubject())
            ));
        } catch (RuntimeException exception) {
            log.warn(
                "Parse result of mail {} is not stored: {}",
                mail.getMessageId(),
                exception.getMessage()
            );
        }
    }

    private MailParsedEvent event(MailMessage mail, TaskDraft draft) {
        return new MailParsedEvent(
            mail.getMessageId(),
            mail.getOrganizationId(),
            mail.getMailboxId(),
            draft.requesterEmail(),
            mail.getSubject(),
            mail.getReceivedAt(),
            draft.title(),
            draft.description(),
            draft.requiresApproval()
        );
    }
}
