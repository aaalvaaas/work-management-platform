package ru.itmo.wmp.platform.api.nextcloud;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import ru.itmo.wmp.platform.IntegrationTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import ru.itmo.wmp.platform.application.nextcloud.NextcloudUser;
import ru.itmo.wmp.platform.application.nextcloud.NextcloudUserGateway;
import ru.itmo.wmp.platform.application.notification.RegistrationNotification;
import ru.itmo.wmp.platform.application.notification.RegistrationNotifier;
import ru.itmo.wmp.platform.domain.organization.Organization;
import ru.itmo.wmp.platform.domain.organization.OrganizationParticipant;
import ru.itmo.wmp.platform.domain.organization.ParticipantStatus;
import ru.itmo.wmp.platform.domain.user.User;
import ru.itmo.wmp.platform.dto.registration.CompleteRegistrationRequest;
import ru.itmo.wmp.platform.dto.registration.StartRegistrationRequest;
import ru.itmo.wmp.platform.exception.nextcloud.NextcloudException;
import ru.itmo.wmp.platform.infrastructure.persistance.organization.JpaOrganizationParticipantRepository;
import ru.itmo.wmp.platform.infrastructure.persistance.organization.JpaOrganizationRepository;
import ru.itmo.wmp.platform.infrastructure.persistance.user.JpaUserRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@IntegrationTest
public class NextcloudAccountControllerIntegrationTest {
    private static final String ISU_NUMBER = "465824";
    private static final String PASSWORD = "Str0ngPassword!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JpaUserRepository userRepository;

    @Autowired
    private JpaOrganizationRepository organizationRepository;

    @Autowired
    private JpaOrganizationParticipantRepository participantRepository;

    @MockitoBean
    private RegistrationNotifier registrationNotifier;

    @MockitoBean
    private NextcloudUserGateway nextcloudUserGateway;

    @Test
    void shouldCreateNextcloudUserOnRegistration() throws Exception {
        register();

        ArgumentCaptor<NextcloudUser> captor = ArgumentCaptor.forClass(NextcloudUser.class);

        verify(nextcloudUserGateway).create(captor.capture(), any());

        assertEquals(ISU_NUMBER, captor.getValue().uid());
        assertEquals("Иванов Иван", captor.getValue().displayName());
        assertEquals("ivanov@niuitmo.ru", captor.getValue().email());

        mockMvc.perform(asAdmin(get("/api/v1/users/{id}/nextcloud", registeredUserId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nextcloudUid").value(ISU_NUMBER))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.lastSyncedAt").exists());
    }

    @Test
    void shouldCompleteRegistrationWhenNextcloudIsUnavailable() throws Exception {
        doThrow(new NextcloudException("Nextcloud is not reachable"))
            .when(nextcloudUserGateway)
            .create(any(NextcloudUser.class), any());

        register();

        mockMvc.perform(asAdmin(get("/api/v1/users/{id}/nextcloud", registeredUserId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.lastError").value("Nextcloud is not reachable"));
    }

    @Test
    void shouldDisableNextcloudUserWhenAccountIsBlocked() throws Exception {
        register();

        Long userId = registeredUserId();

        mockMvc.perform(asAdmin(post("/api/v1/users/{id}/block", userId)))
            .andExpect(status().isOk());

        verify(nextcloudUserGateway).disable(ISU_NUMBER);

        mockMvc.perform(asAdmin(get("/api/v1/users/{id}/nextcloud", userId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DISABLED"));
    }

    @Test
    void shouldEnableNextcloudUserWhenAccountIsUnblocked() throws Exception {
        register();

        Long userId = registeredUserId();

        mockMvc.perform(asAdmin(post("/api/v1/users/{id}/block", userId)))
            .andExpect(status().isOk());

        mockMvc.perform(asAdmin(post("/api/v1/users/{id}/unblock", userId)))
            .andExpect(status().isOk());

        verify(nextcloudUserGateway).enable(ISU_NUMBER);

        mockMvc.perform(asAdmin(get("/api/v1/users/{id}/nextcloud", userId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void shouldRetrySyncFromAdminEndpoint() throws Exception {
        doThrow(new NextcloudException("Nextcloud is not reachable"))
            .when(nextcloudUserGateway)
            .create(any(NextcloudUser.class), any());

        register();

        Long userId = registeredUserId();

        mockMvc.perform(asAdmin(post("/api/v1/users/{id}/nextcloud/sync", userId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.lastError").doesNotExist());

        verify(nextcloudUserGateway).update(any(NextcloudUser.class));
    }

    @Test
    void shouldForbidSyncForRegularUser() throws Exception {
        register();

        Long userId = registeredUserId();

        mockMvc.perform(
            post("/api/v1/users/{id}/nextcloud/sync", userId)
                .with(jwt().jwt(token -> token.subject(String.valueOf(userId))))
        )
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn404WhenAccountHasNoNextcloudUser() throws Exception {
        User user = new User();

        user.setIsuNumber("465825");
        user.setEmail("465825@niuitmo.ru");
        user.setPasswordHash("hash");
        user.setLastName("Петров");
        user.setFirstName("Пётр");

        Long userId = userRepository.save(user).getId();

        mockMvc.perform(asAdmin(get("/api/v1/users/{id}/nextcloud", userId)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("NEXTCLOUD_ACCOUNT_NOT_FOUND"));
    }

    private void register() throws Exception {
        Organization organization = new Organization();

        organization.setName("Мегафакультет");

        OrganizationParticipant participant = new OrganizationParticipant();

        participant.setOrganization(organizationRepository.save(organization));
        participant.setIsuNumber(ISU_NUMBER);
        participant.setLastName("Иванов");
        participant.setFirstName("Иван");
        participant.setEmail("ivanov@niuitmo.ru");
        participant.setStatus(ParticipantStatus.ACTIVE);

        participantRepository.save(participant);

        mockMvc.perform(
            post("/api/v1/registration/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new StartRegistrationRequest(ISU_NUMBER)))
        )
            .andExpect(status().isAccepted());

        ArgumentCaptor<RegistrationNotification> captor =
            ArgumentCaptor.forClass(RegistrationNotification.class);

        verify(registrationNotifier).sendEmailConfirmation(captor.capture());

        mockMvc.perform(
            post("/api/v1/registration/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new CompleteRegistrationRequest(captor.getValue().token(), PASSWORD)
                ))
        )
            .andExpect(status().isCreated());
    }

    private Long registeredUserId() {
        return userRepository.findByIsuNumber(ISU_NUMBER).orElseThrow().getId();
    }

    private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder builder) {
        return builder.with(
            jwt()
                .jwt(token -> token.subject("1"))
                .authorities(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"))
        );
    }
}
