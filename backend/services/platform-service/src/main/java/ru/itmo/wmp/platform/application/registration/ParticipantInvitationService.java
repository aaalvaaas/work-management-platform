package ru.itmo.wmp.platform.application.registration;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.wmp.platform.application.notification.RegistrationNotification;
import ru.itmo.wmp.platform.application.notification.RegistrationNotifier;
import ru.itmo.wmp.platform.application.organization.repository.OrganizationParticipantRepository;
import ru.itmo.wmp.platform.application.user.repository.UserRepository;
import ru.itmo.wmp.platform.domain.organization.OrganizationParticipant;
import ru.itmo.wmp.platform.domain.organization.ParticipantStatus;
import ru.itmo.wmp.platform.domain.registration.InvitationSource;
import ru.itmo.wmp.platform.domain.user.User;
import ru.itmo.wmp.platform.exception.organization.ParticipantNotFoundException;
import ru.itmo.wmp.platform.exception.registration.AlreadyRegisteredException;
import ru.itmo.wmp.platform.exception.registration.RegistrationNotAllowedException;
import ru.itmo.wmp.platform.exception.user.UserNotFoundException;

@Service
@RequiredArgsConstructor
@Transactional
public class ParticipantInvitationService {
    private final OrganizationParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final InvitationService invitationService;
    private final RegistrationNotifier registrationNotifier;

    public IssuedInvitation invite(Long organizationId, String isuNumber, Long invitedByUserId) {
        OrganizationParticipant participant = participantRepository
            .findByOrganizationIdAndIsuNumber(organizationId, isuNumber)
            .orElseThrow(() -> new ParticipantNotFoundException(organizationId, isuNumber));

        if (participant.getStatus() != ParticipantStatus.ACTIVE) {
            throw new RegistrationNotAllowedException(isuNumber);
        }

        if (userRepository.existsByIsuNumber(isuNumber)) {
            throw new AlreadyRegisteredException(isuNumber);
        }

        User invitedBy = userRepository.findById(invitedByUserId)
            .orElseThrow(() -> new UserNotFoundException(invitedByUserId));

        IssuedInvitation issued = invitationService.issue(
            participant,
            InvitationSource.MANAGER_INVITE,
            invitedBy
        );

        registrationNotifier.sendInvitation(
            new RegistrationNotification(
                participant.getEmail(),
                participant.fullName(),
                participant.getIsuNumber(),
                issued.token(),
                issued.invitation().getExpiresAt(),
                participant.getOrganization().getName()
            )
        );

        return issued;
    }
}
