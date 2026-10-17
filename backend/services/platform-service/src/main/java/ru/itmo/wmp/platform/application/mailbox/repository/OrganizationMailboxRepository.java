package ru.itmo.wmp.platform.application.mailbox.repository;

import ru.itmo.wmp.platform.domain.mailbox.MailboxStatus;
import ru.itmo.wmp.platform.domain.mailbox.OrganizationMailbox;

import java.util.List;
import java.util.Optional;

public interface OrganizationMailboxRepository {
    Optional<OrganizationMailbox> findByOrganizationId(Long organizationId);

    List<OrganizationMailbox> findByStatus(MailboxStatus status);

    OrganizationMailbox save(OrganizationMailbox mailbox);

    void delete(OrganizationMailbox mailbox);
}
