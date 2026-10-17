package ru.itmo.wmp.platform.application.registration;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.wmp.platform.application.nextcloud.NextcloudAccountService;
import ru.itmo.wmp.platform.application.notification.RegistrationDecisionNotification;
import ru.itmo.wmp.platform.application.notification.RegistrationNotifier;
import ru.itmo.wmp.platform.application.organization.OrganizationService;
import ru.itmo.wmp.platform.application.organization.repository.OrganizationParticipantRepository;
import ru.itmo.wmp.platform.application.registration.repository.RegistrationRequestRepository;
import ru.itmo.wmp.platform.application.user.UserService;
import ru.itmo.wmp.platform.application.user.repository.UserRepository;
import ru.itmo.wmp.platform.domain.organization.Organization;
import ru.itmo.wmp.platform.domain.organization.OrganizationParticipant;
import ru.itmo.wmp.platform.domain.organization.OrganizationStatus;
import ru.itmo.wmp.platform.domain.organization.ParticipantStatus;
import ru.itmo.wmp.platform.domain.registration.RegistrationRequest;
import ru.itmo.wmp.platform.domain.registration.RegistrationRequestStatus;
import ru.itmo.wmp.platform.domain.user.User;
import ru.itmo.wmp.platform.domain.user.UserStatus;
import ru.itmo.wmp.platform.exception.registration.AlreadyRegisteredException;
import ru.itmo.wmp.platform.exception.registration.EmailAlreadyUsedException;
import ru.itmo.wmp.platform.exception.registration.RegistrationNotAllowedException;
import ru.itmo.wmp.platform.exception.registration.RegistrationRequestAlreadySubmittedException;
import ru.itmo.wmp.platform.exception.registration.RegistrationRequestNotFoundException;
import ru.itmo.wmp.platform.exception.registration.RegistrationRequestNotPendingException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class RegistrationRequestService {
    private final RegistrationRequestRepository requestRepository;
    private final OrganizationService organizationService;
    private final OrganizationParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final NextcloudAccountService nextcloudAccountService;
    private final RegistrationNotifier registrationNotifier;

    public RegistrationRequest submit(RegistrationSubmission submission) {
        Organization organization = organizationService.getByCode(submission.organizationCode());

        if (organization.getStatus() != OrganizationStatus.ACTIVE) {
            throw new RegistrationNotAllowedException(submission.isuNumber());
        }

        if (userRepository.existsByIsuNumber(submission.isuNumber())) {
            throw new AlreadyRegisteredException(submission.isuNumber());
        }

        if (userRepository.existsByEmail(submission.email())) {
            throw new EmailAlreadyUsedException(submission.email());
        }

        boolean pending = requestRepository.existsByOrganizationIdAndIsuNumberAndStatus(
            organization.getId(),
            submission.isuNumber(),
            RegistrationRequestStatus.PENDING
        );

        if (pending) {
            throw new RegistrationRequestAlreadySubmittedException(submission.isuNumber());
        }

        RegistrationRequest request = new RegistrationRequest();

        request.setOrganization(organization);
        request.setIsuNumber(submission.isuNumber());
        request.setLastName(submission.lastName());
        request.setFirstName(submission.firstName());
        request.setPatronymic(submission.patronymic());
        request.setEmail(submission.email());
        request.setPosition(submission.position());
        request.setPasswordHash(passwordEncoder.encode(submission.password()));
        request.setStatus(RegistrationRequestStatus.PENDING);

        return requestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public List<RegistrationRequest> findForOrganization(
        Long organizationId,
        RegistrationRequestStatus status
    ) {
        organizationService.getById(organizationId);

        return status == null
            ? requestRepository.findByOrganizationIdOrderByIdDesc(organizationId)
            : requestRepository.findByOrganizationIdAndStatusOrderByIdDesc(organizationId, status);
    }

    public RegistrationRequest approve(Long organizationId, Long requestId, Long actorUserId) {
        RegistrationRequest request = pendingRequest(organizationId, requestId);

        if (userRepository.existsByIsuNumber(request.getIsuNumber())) {
            throw new AlreadyRegisteredException(request.getIsuNumber());
        }

        User user = userRepository.save(accountOf(request));

        linkParticipant(request, user);

        decide(request, RegistrationRequestStatus.APPROVED, null, actorUserId);

        nextcloudAccountService.provision(user);

        notifyDecision(request);

        return request;
    }

    public RegistrationRequest reject(
        Long organizationId,
        Long requestId,
        String comment,
        Long actorUserId
    ) {
        RegistrationRequest request = pendingRequest(organizationId, requestId);

        decide(request, RegistrationRequestStatus.REJECTED, comment, actorUserId);

        notifyDecision(request);

        return request;
    }

    private RegistrationRequest pendingRequest(Long organizationId, Long requestId) {
        RegistrationRequest request = requestRepository.findById(requestId)
            .filter(found -> Objects.equals(found.getOrganization().getId(), organizationId))
            .orElseThrow(() -> new RegistrationRequestNotFoundException(organizationId, requestId));

        if (!request.isPending()) {
            throw new RegistrationRequestNotPendingException(requestId, request.getStatus().name());
        }

        return request;
    }

    private void decide(
        RegistrationRequest request,
        RegistrationRequestStatus status,
        String comment,
        Long actorUserId
    ) {
        request.setStatus(status);
        request.setDecisionComment(comment);
        request.setDecidedAt(LocalDateTime.now());
        request.setDecidedBy(userService.getById(actorUserId));

        requestRepository.save(request);
    }

    private void linkParticipant(RegistrationRequest request, User user) {
        OrganizationParticipant participant = participantRepository
            .findByOrganizationIdAndIsuNumber(
                request.getOrganization().getId(),
                request.getIsuNumber()
            )
            .orElseGet(() -> newParticipant(request));

        participant.setUser(user);
        participant.setStatus(ParticipantStatus.ACTIVE);

        participantRepository.save(participant);
    }

    private OrganizationParticipant newParticipant(RegistrationRequest request) {
        OrganizationParticipant participant = new OrganizationParticipant();

        participant.setOrganization(request.getOrganization());
        participant.setIsuNumber(request.getIsuNumber());
        participant.setLastName(request.getLastName());
        participant.setFirstName(request.getFirstName());
        participant.setPatronymic(request.getPatronymic());
        participant.setEmail(request.getEmail());
        participant.setPosition(request.getPosition());

        return participant;
    }

    private User accountOf(RegistrationRequest request) {
        User user = new User();

        user.setIsuNumber(request.getIsuNumber());
        user.setEmail(request.getEmail());
        user.setLastName(request.getLastName());
        user.setFirstName(request.getFirstName());
        user.setPatronymic(request.getPatronymic());
        user.setPosition(request.getPosition());
        user.setPasswordHash(request.getPasswordHash());
        user.setStatus(UserStatus.ACTIVE);

        return user;
    }

    private void notifyDecision(RegistrationRequest request) {
        registrationNotifier.sendRegistrationDecision(new RegistrationDecisionNotification(
            request.getEmail(),
            request.fullName(),
            request.getIsuNumber(),
            request.getOrganization().getName(),
            request.getStatus() == RegistrationRequestStatus.APPROVED,
            request.getDecisionComment()
        ));
    }
}
