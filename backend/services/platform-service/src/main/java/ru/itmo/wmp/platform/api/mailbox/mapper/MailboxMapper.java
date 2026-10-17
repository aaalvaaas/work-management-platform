package ru.itmo.wmp.platform.api.mailbox.mapper;

import org.springframework.stereotype.Component;
import ru.itmo.wmp.platform.domain.mailbox.OrganizationMailbox;
import ru.itmo.wmp.platform.dto.mailbox.MailboxResponse;

@Component
public class MailboxMapper {
    public MailboxResponse toResponse(OrganizationMailbox mailbox) {
        return new MailboxResponse(
            mailbox.getId(),
            mailbox.getOrganization().getId(),
            mailbox.getHost(),
            mailbox.getPort(),
            mailbox.getUsername(),
            mailbox.getFolder(),
            mailbox.isUseSsl(),
            mailbox.getStatus(),
            mailbox.getLastError(),
            mailbox.getLastCheckedAt()
        );
    }
}
