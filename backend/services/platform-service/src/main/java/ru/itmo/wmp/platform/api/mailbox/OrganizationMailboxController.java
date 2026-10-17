package ru.itmo.wmp.platform.api.mailbox;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.wmp.platform.api.mailbox.mapper.MailboxMapper;
import ru.itmo.wmp.platform.application.mailbox.MailboxSettings;
import ru.itmo.wmp.platform.application.mailbox.OrganizationMailboxService;
import ru.itmo.wmp.platform.dto.mailbox.ConnectMailboxRequest;
import ru.itmo.wmp.platform.dto.mailbox.MailboxResponse;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/mailbox")
@PreAuthorize("@accessGuard.isOrganizationAdmin(#organizationId, authentication)")
@RequiredArgsConstructor
public class OrganizationMailboxController {
    private final OrganizationMailboxService mailboxService;
    private final MailboxMapper mailboxMapper;

    @PutMapping
    @Operation(
        summary = "Connect the mailbox of the organization",
        description = "The password is stored encrypted and is never returned"
    )
    public MailboxResponse connect(
        @PathVariable Long organizationId,
        @Valid @RequestBody ConnectMailboxRequest request
    ) {
        return mailboxMapper.toResponse(
            mailboxService.connect(
                organizationId,
                new MailboxSettings(
                    request.host(),
                    request.port(),
                    request.username(),
                    request.password(),
                    request.folder(),
                    request.useSsl()
                )
            )
        );
    }

    @GetMapping
    @Operation(summary = "Read the mailbox settings of the organization")
    public MailboxResponse get(@PathVariable Long organizationId) {
        return mailboxMapper.toResponse(mailboxService.getByOrganization(organizationId));
    }

    @PostMapping("/disable")
    @Operation(
        summary = "Stop reading the mailbox",
        description = "Settings are kept, mail is no longer imported"
    )
    public MailboxResponse disable(@PathVariable Long organizationId) {
        return mailboxMapper.toResponse(mailboxService.disable(organizationId));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Forget the mailbox and its password")
    public void disconnect(@PathVariable Long organizationId) {
        mailboxService.disconnect(organizationId);
    }
}
