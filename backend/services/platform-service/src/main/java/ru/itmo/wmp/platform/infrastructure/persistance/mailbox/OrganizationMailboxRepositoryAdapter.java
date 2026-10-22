package ru.itmo.wmp.platform.infrastructure.persistance.mailbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import ru.itmo.wmp.platform.application.mailbox.repository.OrganizationMailboxRepository;
import ru.itmo.wmp.platform.domain.mailbox.MailboxStatus;
import ru.itmo.wmp.platform.domain.mailbox.OrganizationMailbox;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OrganizationMailboxRepositoryAdapter implements OrganizationMailboxRepository {
    private final JpaOrganizationMailboxRepository mailboxRepository;

    @Override
    public Optional<OrganizationMailbox> findById(Long id) {
        return mailboxRepository.findById(id);
    }

    @Override
    public Optional<OrganizationMailbox> findByOrganizationId(Long organizationId) {
        return mailboxRepository.findByOrganizationId(organizationId);
    }

    @Override
    public List<OrganizationMailbox> findByStatusIn(List<MailboxStatus> statuses) {
        return mailboxRepository.findByStatusIn(statuses);
    }

    @Override
    public OrganizationMailbox save(OrganizationMailbox mailbox) {
        return mailboxRepository.save(mailbox);
    }

    @Override
    public void delete(OrganizationMailbox mailbox) {
        mailboxRepository.delete(mailbox);
    }
}
