package ru.itmo.wmp.mail.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.itmo.wmp.mail.api.mapper.MailMapper;
import ru.itmo.wmp.mail.application.MailService;
import ru.itmo.wmp.mail.domain.MailMessage;
import ru.itmo.wmp.mail.dto.request.MailRequest;
import ru.itmo.wmp.mail.dto.response.MailResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mails")
@RequiredArgsConstructor
public class MailController {
    private final MailService mailService;
    private final MailMapper mailMapper;

    @PostMapping
    public MailResponse receiveMail(
        @Valid @RequestBody MailRequest request) {
        MailMessage mail = mailMapper.toEntity(request);

        MailMessage savedMail = mailService.receiveMail(mail);

        return mailMapper.toResponse(savedMail);
    }

    @GetMapping
    public List<MailResponse> getAllMails() {
        return mailService.getAll()
            .stream()
            .map(mailMapper::toResponse)
            .toList();
    }

    @GetMapping("/{messageId}")
    public MailResponse getMailByMessageId(
        @PathVariable String messageId
    ) {
        MailMessage mail = mailService.getMail(messageId);

        return mailMapper.toResponse(mail);
    }

    @PatchMapping("/{messageId}/processing")
    public MailResponse markProcessing(
        @PathVariable String messageId
    ) {
        return mailMapper.toResponse(mailService.markAsProcessing(messageId));
    }

    @PatchMapping("/{messageId}/processed")
    public MailResponse markProcessed(
        @PathVariable String messageId
    ) {
        return mailMapper.toResponse(mailService.markAsProcessed(messageId));
    }

    @PatchMapping("/{messageId}/failed")
    public MailResponse markFailed(
        @PathVariable String messageId
    ) {
        return mailMapper.toResponse(mailService.markAsFailed(messageId));
    }
}
