package ru.itmo.wmp.platform.application.registration;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.wmp.platform.application.organization.repository.OrganizationParticipantRepository;
import ru.itmo.wmp.platform.application.user.repository.UserRepository;
import ru.itmo.wmp.platform.domain.organization.OrganizationParticipant;
import ru.itmo.wmp.platform.domain.organization.ParticipantStatus;
import ru.itmo.wmp.platform.exception.registration.AlreadyRegisteredException;
import ru.itmo.wmp.platform.exception.registration.RegistrationNotAllowedException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegistrationEligibilityService {
    private final UserRepository userRepository;
    private final OrganizationParticipantRepository participantRepository;

    public List<OrganizationParticipant> eligibleParticipants(String isuNumber) {
        if (userRepository.existsByIsuNumber(isuNumber)) {
            throw new AlreadyRegisteredException(isuNumber);
        }

        List<OrganizationParticipant> participants = participantRepository
            .findByIsuNumberAndStatus(isuNumber, ParticipantStatus.ACTIVE);

        if (participants.isEmpty()) {
            throw new RegistrationNotAllowedException(isuNumber);
        }

        return participants;
    }
}
