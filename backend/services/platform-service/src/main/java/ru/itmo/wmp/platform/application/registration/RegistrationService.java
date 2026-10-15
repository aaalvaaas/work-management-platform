package ru.itmo.wmp.platform.application.registration;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.wmp.platform.application.nextcloud.NextcloudAccountService;
import ru.itmo.wmp.platform.application.notification.RegistrationNotification;
import ru.itmo.wmp.platform.application.notification.RegistrationNotifier;
import ru.itmo.wmp.platform.application.organization.repository.OrganizationParticipantRepository;
import ru.itmo.wmp.platform.application.user.repository.UserRepository;
import ru.itmo.wmp.platform.domain.organization.OrganizationParticipant;
import ru.itmo.wmp.platform.domain.registration.Invitation;
import ru.itmo.wmp.platform.domain.registration.InvitationSource;
import ru.itmo.wmp.platform.domain.user.User;
import ru.itmo.wmp.platform.domain.user.UserStatus;
import ru.itmo.wmp.platform.exception.registration.EmailAlreadyUsedException;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class RegistrationService {
    private final RegistrationEligibilityService eligibilityService;
    private final InvitationService invitationService;
    private final RegistrationNotifier registrationNotifier;
    private final UserRepository userRepository;
    private final OrganizationParticipantRepository participantRepository;
    private final PasswordEncoder passwordEncoder;
    private final NextcloudAccountService nextcloudAccountService;

    public RegistrationStart start(String isuNumber) {
        List<OrganizationParticipant> participants = eligibilityService
            .eligibleParticipants(isuNumber);

        OrganizationParticipant participant = participants.getFirst();

        IssuedInvitation issued = invitationService.issue(
            participant,
            InvitationSource.SELF_SERVICE,
            null
        );

        registrationNotifier.sendEmailConfirmation(notificationOf(issued, participant));

        return new RegistrationStart(
            participant.getEmail(),
            issued.invitation().getExpiresAt()
        );
    }

    @Transactional(readOnly = true)
    public RegistrationDetails details(String token) {
        Invitation invitation = invitationService.verify(token);

        List<OrganizationParticipant> participants = eligibilityService
            .eligibleParticipants(invitation.getIsuNumber());

        OrganizationParticipant participant = profileSource(invitation, participants);

        return new RegistrationDetails(
            participant.getIsuNumber(),
            participant.getLastName(),
            participant.getFirstName(),
            participant.getPatronymic(),
            participant.getEmail(),
            participants.stream()
                .map(member -> member.getOrganization().getName())
                .toList()
        );
    }

    public User complete(String token, String password) {
        Invitation invitation = invitationService.verify(token);

        List<OrganizationParticipant> participants = eligibilityService
            .eligibleParticipants(invitation.getIsuNumber());

        OrganizationParticipant participant = profileSource(invitation, participants);

        if (userRepository.existsByEmail(participant.getEmail())) {
            throw new EmailAlreadyUsedException(participant.getEmail());
        }

        User user = userRepository.save(accountOf(participant, password));

        participants.forEach(member -> member.setUser(user));

        participantRepository.saveAll(participants);

        invitationService.markUsed(invitation);
        invitationService.invalidatePendingFor(invitation.getIsuNumber());

        nextcloudAccountService.provision(user);

        return user;
    }

    private User accountOf(OrganizationParticipant participant, String password) {
        User user = new User();

        user.setIsuNumber(participant.getIsuNumber());
        user.setEmail(participant.getEmail());
        user.setLastName(participant.getLastName());
        user.setFirstName(participant.getFirstName());
        user.setPatronymic(participant.getPatronymic());
        user.setPosition(participant.getPosition());
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setStatus(UserStatus.ACTIVE);

        return user;
    }

    private OrganizationParticipant profileSource(
        Invitation invitation,
        List<OrganizationParticipant> participants
    ) {
        OrganizationParticipant invited = invitation.getParticipant();

        if (invited == null) {
            return participants.getFirst();
        }

        return participants.stream()
            .filter(member -> Objects.equals(member.getId(), invited.getId()))
            .findFirst()
            .orElseGet(participants::getFirst);
    }

    private RegistrationNotification notificationOf(
        IssuedInvitation issued,
        OrganizationParticipant participant
    ) {
        return new RegistrationNotification(
            participant.getEmail(),
            participant.fullName(),
            participant.getIsuNumber(),
            issued.token(),
            issued.invitation().getExpiresAt(),
            participant.getOrganization().getName()
        );
    }
}
