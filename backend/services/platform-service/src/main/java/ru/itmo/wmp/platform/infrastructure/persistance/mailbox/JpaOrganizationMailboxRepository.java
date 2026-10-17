package ru.itmo.wmp.platform.infrastructure.persistance.mailbox;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.wmp.platform.domain.mailbox.MailboxStatus;
import ru.itmo.wmp.platform.domain.mailbox.OrganizationMailbox;

import java.util.List;
import java.util.Optional;

public interface JpaOrganizationMailboxRepository extends JpaRepository<OrganizationMailbox, Long> {
    Optional<OrganizationMailbox> findByOrganizationId(Long organizationId);

    List<OrganizationMailbox> findByStatus(MailboxStatus status);
}
