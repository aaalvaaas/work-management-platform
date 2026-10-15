package ru.itmo.wmp.platform.application.registration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.wmp.platform.application.organization.repository.OrganizationParticipantRepository;
import ru.itmo.wmp.platform.application.user.repository.UserRepository;
import ru.itmo.wmp.platform.domain.organization.OrganizationParticipant;
import ru.itmo.wmp.platform.domain.organization.ParticipantStatus;
import ru.itmo.wmp.platform.exception.registration.AlreadyRegisteredException;
import ru.itmo.wmp.platform.exception.registration.RegistrationNotAllowedException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RegistrationEligibilityServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationParticipantRepository participantRepository;

    @InjectMocks
    private RegistrationEligibilityService eligibilityService;

    @Test
    void shouldReturnParticipantsFromAllOrganizations() {
        when(userRepository.existsByIsuNumber("465824"))
            .thenReturn(false);

        when(participantRepository.findByIsuNumberAndStatus("465824", ParticipantStatus.ACTIVE))
            .thenReturn(List.of(new OrganizationParticipant(), new OrganizationParticipant()));

        assertEquals(2, eligibilityService.eligibleParticipants("465824").size());
    }

    @Test
    void shouldRejectIsuNumberMissingInAllLists() {
        when(userRepository.existsByIsuNumber("465824"))
            .thenReturn(false);

        when(participantRepository.findByIsuNumberAndStatus("465824", ParticipantStatus.ACTIVE))
            .thenReturn(List.of());

        assertThrows(
            RegistrationNotAllowedException.class,
            () -> eligibilityService.eligibleParticipants("465824")
        );
    }

    @Test
    void shouldRejectAlreadyRegisteredIsuNumber() {
        when(userRepository.existsByIsuNumber("465824"))
            .thenReturn(true);

        assertThrows(
            AlreadyRegisteredException.class,
            () -> eligibilityService.eligibleParticipants("465824")
        );

        verify(participantRepository, never())
            .findByIsuNumberAndStatus(any(), any());
    }
}
