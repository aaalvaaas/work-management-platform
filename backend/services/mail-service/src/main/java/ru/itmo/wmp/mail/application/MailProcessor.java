package ru.itmo.wmp.mail.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.itmo.wmp.mail.application.event.MailEventPublisher;
import ru.itmo.wmp.mail.application.event.MailParsedEvent;
import ru.itmo.wmp.mail.application.parse.TaskDraft;
import ru.itmo.wmp.mail.application.parse.TaskDraftParser;
import ru.itmo.wmp.mail.domain.MailMessage;

@Service
@RequiredArgsConstructor
public class MailProcessor {
    private final MailService mailService;
    private final TaskDraftParser taskDraftParser;
    private final MailEventPublisher mailEventPublisher;

    public void process(MailMessage mail) {
        String messageId = mail.getMessageId();

        mailService.markAsProcessing(messageId);

        try {
            mailEventPublisher.publishParsed(parsed(mail));

            mailService.markAsProcessed(messageId);
        } catch (Exception e) {
            mailService.markAsFailed(messageId);

            throw e;
        }
    }

    private MailParsedEvent parsed(MailMessage mail) {
        TaskDraft draft = taskDraftParser.parse(mail);

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
