package ru.itmo.wmp.platform.api.registration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.wmp.platform.application.notification.RegistrationNotification;
import ru.itmo.wmp.platform.application.notification.RegistrationNotifier;
import ru.itmo.wmp.platform.domain.organization.Organization;
import ru.itmo.wmp.platform.domain.organization.OrganizationParticipant;
import ru.itmo.wmp.platform.domain.organization.ParticipantStatus;
import ru.itmo.wmp.platform.domain.user.User;
import ru.itmo.wmp.platform.dto.registration.CompleteRegistrationRequest;
import ru.itmo.wmp.platform.dto.registration.CreateInvitationRequest;
import ru.itmo.wmp.platform.infrastructure.persistance.organization.JpaOrganizationParticipantRepository;
import ru.itmo.wmp.platform.infrastructure.persistance.organization.JpaOrganizationRepository;
import ru.itmo.wmp.platform.infrastructure.persistance.user.JpaUserRepository;

import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class InvitationControllerIntegrationTest {
    private static final String ISU_NUMBER = "465824";
    private static final String ADMIN_ISU_NUMBER = "465900";

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

    @Test
    void shouldRejectInvitationFromRegularUser() throws Exception {
        Organization organization = organization("Мегафакультет");
        participant(organization, ISU_NUMBER);

        User user = userRepository.save(account(ADMIN_ISU_NUMBER));

        mockMvc.perform(
            inviteRequest(organization.getId(), ISU_NUMBER)
                .with(jwt().jwt(token -> token.subject(String.valueOf(user.getId()))))
        )
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldCreateSingleUseInvitationForParticipant() throws Exception {
        Organization organization = organization("Мегафакультет");
        participant(organization, ISU_NUMBER);

        mockMvc.perform(asAdmin(inviteRequest(organization.getId(), ISU_NUMBER)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.isuNumber").value(ISU_NUMBER))
            .andExpect(jsonPath("$.email").value("i***@niuitmo.ru"))
            .andExpect(jsonPath("$.source").value("MANAGER_INVITE"))
            .andExpect(jsonPath("$.organizationId").value(organization.getId()))
            .andExpect(jsonPath("$.expiresAt").exists());
    }

    @Test
    void shouldAllowRegistrationByInvitationToken() throws Exception {
        Organization organization = organization("Мегафакультет");
        participant(organization, ISU_NUMBER);

        mockMvc.perform(asAdmin(inviteRequest(organization.getId(), ISU_NUMBER)))
            .andExpect(status().isCreated());

        ArgumentCaptor<RegistrationNotification> captor =
            ArgumentCaptor.forClass(RegistrationNotification.class);

        verify(registrationNotifier).sendInvitation(captor.capture());

        String token = captor.getValue().token();

        mockMvc.perform(
            post("/api/v1/registration/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new CompleteRegistrationRequest(token, "Str0ngPassword!")
                ))
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.isuNumber").value(ISU_NUMBER));
    }

    @Test
    void shouldRejectInvitationForPersonOutsideOrganizationList() throws Exception {
        Organization organization = organization("Мегафакультет");

        mockMvc.perform(asAdmin(inviteRequest(organization.getId(), ISU_NUMBER)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("PARTICIPANT_NOT_FOUND"));
    }

    @Test
    void shouldRejectInvitationForExcludedParticipant() throws Exception {
        Organization organization = organization("Мегафакультет");

        OrganizationParticipant participant = participant(organization, ISU_NUMBER);

        participant.setStatus(ParticipantStatus.INACTIVE);
        participantRepository.save(participant);

        mockMvc.perform(asAdmin(inviteRequest(organization.getId(), ISU_NUMBER)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("REGISTRATION_NOT_ALLOWED"));
    }

    @Test
    void shouldRejectInvitationForRegisteredAccount() throws Exception {
        Organization organization = organization("Мегафакультет");
        participant(organization, ISU_NUMBER);

        userRepository.save(account(ISU_NUMBER));

        mockMvc.perform(asAdmin(inviteRequest(organization.getId(), ISU_NUMBER)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("ALREADY_REGISTERED"));
    }

    private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder builder) {
        User admin = userRepository.save(account(ADMIN_ISU_NUMBER));

        return builder.with(
            jwt()
                .jwt(token -> token.subject(String.valueOf(admin.getId())))
                .authorities(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"))
        );
    }

    private MockHttpServletRequestBuilder inviteRequest(
        Long organizationId,
        String isuNumber
    ) throws Exception {
        return post("/api/v1/organizations/{organizationId}/invitations", organizationId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new CreateInvitationRequest(isuNumber)));
    }

    private Organization organization(String name) {
        Organization organization = new Organization();

        organization.setName(name);

        return organizationRepository.save(organization);
    }

    private OrganizationParticipant participant(Organization organization, String isuNumber) {
        OrganizationParticipant participant = new OrganizationParticipant();

        participant.setOrganization(organization);
        participant.setIsuNumber(isuNumber);
        participant.setLastName("Иванов");
        participant.setFirstName("Иван");
        participant.setEmail("ivanov@niuitmo.ru");
        participant.setStatus(ParticipantStatus.ACTIVE);

        return participantRepository.save(participant);
    }

    private User account(String isuNumber) {
        User user = new User();

        user.setIsuNumber(isuNumber);
        user.setEmail(isuNumber + "@niuitmo.ru");
        user.setPasswordHash("hash");
        user.setLastName("Петров");
        user.setFirstName("Пётр");

        return user;
    }
}
