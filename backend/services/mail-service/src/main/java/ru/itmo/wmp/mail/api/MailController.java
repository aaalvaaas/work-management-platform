package ru.itmo.wmp.mail.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itmo.wmp.mail.api.mapper.MailAttachmentMapper;
import ru.itmo.wmp.mail.api.mapper.MailMapper;
import ru.itmo.wmp.mail.application.attachment.MailAttachmentDownload;
import ru.itmo.wmp.mail.application.attachment.MailAttachmentService;
import ru.itmo.wmp.mail.application.MailService;
import ru.itmo.wmp.mail.domain.MailMessage;
import ru.itmo.wmp.mail.dto.request.MailRequest;
import ru.itmo.wmp.mail.dto.response.MailAttachmentResponse;
import ru.itmo.wmp.mail.dto.response.MailResponse;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/mails")
@RequiredArgsConstructor
public class MailController {
    private final MailService mailService;
    private final MailAttachmentService mailAttachmentService;
    private final MailMapper mailMapper;
    private final MailAttachmentMapper mailAttachmentMapper;

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

    @GetMapping("/{messageId}/attachments")
    public List<MailAttachmentResponse> getMailAttachments(
        @PathVariable String messageId
    ) {
        MailMessage mail = mailService.getMail(messageId);

        return mailAttachmentService
            .findAllByMailMessageId(mail.getId())
            .stream()
            .map(mailAttachmentMapper::toResponse)
            .toList();
    }

    @GetMapping("/{messageId}/attachments/{attachmentId}")
    public ResponseEntity<Resource> downloadAttachment(
        @PathVariable String messageId,
        @PathVariable Long attachmentId
    ) {
        MailMessage mail = mailService.getMail(messageId);

        MailAttachmentDownload download = mailAttachmentService.download(mail.getId(), attachmentId);

        InputStreamResource resource = new InputStreamResource(download.inputStream());

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(download.contentType()))
            .contentLength(download.size())
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment()
                    .filename(download.filename(), StandardCharsets.UTF_8)
                    .build()
                    .toString()
            )
            .body(resource);
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
