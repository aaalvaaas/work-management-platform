package ru.itmo.wmp.platform.api.internal;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.wmp.platform.application.mailbox.OrganizationMailboxService;
import ru.itmo.wmp.platform.domain.mailbox.OrganizationMailbox;
import ru.itmo.wmp.platform.dto.mailbox.InternalMailboxResponse;

import java.util.List;

@RestController
@RequestMapping("/internal/v1/mailboxes")
@RequiredArgsConstructor
public class InternalMailboxController {
    private final OrganizationMailboxService mailboxService;

    @GetMapping
    @Operation(
        summary = "Mailboxes to read, for services",
        description = "Protected by the internal key and never routed through the gateway"
    )
    public List<InternalMailboxResponse> activeMailboxes() {
        return mailboxService.activeMailboxes()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    private InternalMailboxResponse toResponse(OrganizationMailbox mailbox) {
        return new InternalMailboxResponse(
            mailbox.getId(),
            mailbox.getOrganization().getId(),
            mailbox.getOrganization().getCode(),
            mailbox.getHost(),
            mailbox.getPort(),
            mailbox.getUsername(),
            mailboxService.passwordOf(mailbox),
            mailbox.getFolder(),
            mailbox.isUseSsl()
        );
    }
}
