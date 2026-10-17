package ru.itmo.wmp.platform.application.mailbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.wmp.platform.application.mailbox.repository.OrganizationMailboxRepository;
import ru.itmo.wmp.platform.application.organization.OrganizationService;
import ru.itmo.wmp.platform.application.security.SecretCipher;
import ru.itmo.wmp.platform.domain.mailbox.MailboxStatus;
import ru.itmo.wmp.platform.domain.mailbox.OrganizationMailbox;
import ru.itmo.wmp.platform.exception.mailbox.MailboxNotFoundException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrganizationMailboxService {
    private static final String DEFAULT_FOLDER = "INBOX";

    private final OrganizationMailboxRepository mailboxRepository;
    private final OrganizationService organizationService;
    private final SecretCipher secretCipher;

    public OrganizationMailbox connect(Long organizationId, MailboxSettings settings) {
        OrganizationMailbox mailbox = mailboxRepository.findByOrganizationId(organizationId)
            .orElseGet(OrganizationMailbox::new);

        mailbox.setOrganization(organizationService.getById(organizationId));
        mailbox.setHost(settings.host());
        mailbox.setPort(settings.port());
        mailbox.setUsername(settings.username());
        mailbox.setFolder(settings.folder() == null ? DEFAULT_FOLDER : settings.folder());
        mailbox.setUseSsl(settings.useSsl());
        mailbox.setStatus(MailboxStatus.ACTIVE);
        mailbox.setLastError(null);

        if (settings.password() != null && !settings.password().isBlank()) {
            mailbox.setPasswordCipher(secretCipher.encrypt(settings.password()));
        }

        if (mailbox.getPasswordCipher() == null) {
            throw new IllegalArgumentException("Password is required when connecting a mailbox");
        }

        return mailboxRepository.save(mailbox);
    }

    @Transactional(readOnly = true)
    public OrganizationMailbox getByOrganization(Long organizationId) {
        return mailboxRepository.findByOrganizationId(organizationId)
            .orElseThrow(() -> new MailboxNotFoundException(organizationId));
    }

    @Transactional(readOnly = true)
    public List<OrganizationMailbox> activeMailboxes() {
        return mailboxRepository.findByStatus(MailboxStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public String passwordOf(OrganizationMailbox mailbox) {
        return secretCipher.decrypt(mailbox.getPasswordCipher());
    }

    public OrganizationMailbox disable(Long organizationId) {
        OrganizationMailbox mailbox = getByOrganization(organizationId);

        mailbox.setStatus(MailboxStatus.DISABLED);

        return mailboxRepository.save(mailbox);
    }

    public void disconnect(Long organizationId) {
        mailboxRepository.delete(getByOrganization(organizationId));
    }

    public void recordCheck(Long mailboxId, boolean successful, String error) {
        OrganizationMailbox mailbox = mailboxRepository.findById(mailboxId)
            .orElseThrow(() -> new MailboxNotFoundException(mailboxId));

        if (mailbox.getStatus() == MailboxStatus.DISABLED) {
            return;
        }

        mailbox.setStatus(successful ? MailboxStatus.ACTIVE : MailboxStatus.ERROR);
        mailbox.setLastError(successful ? null : error);
        mailbox.setLastCheckedAt(LocalDateTime.now());

        mailboxRepository.save(mailbox);
    }
}
