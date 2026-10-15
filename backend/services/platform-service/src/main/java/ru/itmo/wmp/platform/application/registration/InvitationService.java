package ru.itmo.wmp.platform.application.registration;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.wmp.platform.application.registration.repository.InvitationRepository;
import ru.itmo.wmp.platform.application.security.SecureTokenService;
import ru.itmo.wmp.platform.config.properties.RegistrationProperties;
import ru.itmo.wmp.platform.domain.organization.OrganizationParticipant;
import ru.itmo.wmp.platform.domain.registration.Invitation;
import ru.itmo.wmp.platform.domain.registration.InvitationSource;
import ru.itmo.wmp.platform.domain.user.User;
import ru.itmo.wmp.platform.exception.registration.InvitationNotValidException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InvitationService {
    private final InvitationRepository invitationRepository;
    private final SecureTokenService secureTokenService;
    private final RegistrationProperties registrationProperties;

    public IssuedInvitation issue(
        OrganizationParticipant participant,
        InvitationSource source,
        User createdBy
    ) {
        String token = secureTokenService.generate();

        Invitation invitation = new Invitation();

        invitation.setIsuNumber(participant.getIsuNumber());
        invitation.setEmail(participant.getEmail());
        invitation.setOrganization(participant.getOrganization());
        invitation.setParticipant(participant);
        invitation.setSource(source);
        invitation.setTokenHash(secureTokenService.hash(token));
        invitation.setExpiresAt(LocalDateTime.now().plus(ttlOf(source)));
        invitation.setCreatedBy(createdBy);

        return new IssuedInvitation(invitationRepository.save(invitation), token);
    }

    public Invitation verify(String token) {
        Invitation invitation = invitationRepository
            .findByTokenHash(secureTokenService.hash(token))
            .orElseThrow(InvitationNotValidException::new);

        if (!invitation.isUsable(LocalDateTime.now())) {
            throw new InvitationNotValidException();
        }

        return invitation;
    }

    public void markUsed(Invitation invitation) {
        invitation.setUsedAt(LocalDateTime.now());

        invitationRepository.save(invitation);
    }

    public void invalidatePendingFor(String isuNumber) {
        LocalDateTime now = LocalDateTime.now();

        List<Invitation> pending = invitationRepository
            .findByIsuNumberAndUsedAtIsNull(isuNumber);

        pending.forEach(invitation -> invitation.setUsedAt(now));

        invitationRepository.saveAll(pending);
    }

    private Duration ttlOf(InvitationSource source) {
        return source == InvitationSource.MANAGER_INVITE
            ? registrationProperties.invitationTtl()
            : registrationProperties.emailConfirmationTtl();
    }
}
