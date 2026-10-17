package ru.itmo.wmp.platform.api.mailbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import ru.itmo.wmp.platform.IntegrationTest;
import ru.itmo.wmp.platform.application.mailbox.repository.OrganizationMailboxRepository;
import ru.itmo.wmp.platform.application.security.SecretCipher;
import ru.itmo.wmp.platform.domain.organization.Organization;
import ru.itmo.wmp.platform.domain.organization.OrganizationParticipant;
import ru.itmo.wmp.platform.domain.organization.ParticipantStatus;
import ru.itmo.wmp.platform.domain.user.User;
import ru.itmo.wmp.platform.domain.user.UserStatus;
import ru.itmo.wmp.platform.dto.mailbox.ConnectMailboxRequest;
import ru.itmo.wmp.platform.dto.organization.AssignOrganizationRoleRequest;
import ru.itmo.wmp.platform.infrastructure.persistance.organization.JpaOrganizationParticipantRepository;
import ru.itmo.wmp.platform.infrastructure.persistance.organization.JpaOrganizationRepository;
import ru.itmo.wmp.platform.infrastructure.persistance.user.JpaUserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
public class OrganizationMailboxIntegrationTest {
    private static final String ADMIN_ISU = "100500";
    private static final String MANAGER_ISU = "465824";
    private static final String MEMBER_ISU = "465825";
    private static final String PASSWORD = "mailbox-password";

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

    @Autowired
    private OrganizationMailboxRepository mailboxRepository;

    @Autowired
    private SecretCipher secretCipher;

    @Test
    void shouldConnectMailboxAndHideThePassword() throws Exception {
        Organization organization = organization();
        User manager = manager(organization);

        mockMvc.perform(connectRequest(organization, manager, PASSWORD))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.host").value("imap.mail.ru"))
            .andExpect(jsonPath("$.username").value("megafaculty@mail.ru"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.password").doesNotExist());

        String stored = mailboxRepository.findByOrganizationId(organization.getId())
            .orElseThrow()
            .getPasswordCipher();

        assertThat(stored).isNotEqualTo(PASSWORD);
        assertThat(secretCipher.decrypt(stored)).isEqualTo(PASSWORD);
    }

    @Test
    void shouldKeepThePasswordWhenSettingsAreUpdatedWithoutIt() throws Exception {
        Organization organization = organization();
        User manager = manager(organization);

        mockMvc.perform(connectRequest(organization, manager, PASSWORD))
            .andExpect(status().isOk());

        mockMvc.perform(connectRequest(organization, manager, null))
            .andExpect(status().isOk());

        String stored = mailboxRepository.findByOrganizationId(organization.getId())
            .orElseThrow()
            .getPasswordCipher();

        assertThat(secretCipher.decrypt(stored)).isEqualTo(PASSWORD);
    }

    @Test
    void shouldAnswerNotFoundUntilMailboxIsConnected() throws Exception {
        Organization organization = organization();
        User manager = manager(organization);

        mockMvc.perform(
            get("/api/v1/organizations/{id}/mailbox", organization.getId()).with(asUser(manager))
        )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("MAILBOX_NOT_FOUND"));
    }

    @Test
    void shouldDisableAndDisconnectMailbox() throws Exception {
        Organization organization = organization();
        User manager = manager(organization);

        mockMvc.perform(connectRequest(organization, manager, PASSWORD))
            .andExpect(status().isOk());

        mockMvc.perform(
            post("/api/v1/organizations/{id}/mailbox/disable", organization.getId())
                .with(asUser(manager))
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DISABLED"));

        mockMvc.perform(
            delete("/api/v1/organizations/{id}/mailbox", organization.getId())
                .with(asUser(manager))
        )
            .andExpect(status().isNoContent());

        assertThat(mailboxRepository.findByOrganizationId(organization.getId())).isEmpty();
    }

    @Test
    void shouldForbidMailboxSettingsForRegularParticipant() throws Exception {
        Organization organization = organization();
        User member = account(MEMBER_ISU);

        participant(organization, MEMBER_ISU, member);

        mockMvc.perform(connectRequest(organization, member, PASSWORD))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldValidateMailboxSettings() throws Exception {
        Organization organization = organization();
        User manager = manager(organization);

        ConnectMailboxRequest request = new ConnectMailboxRequest("", 0, "", PASSWORD, null, true);

        mockMvc.perform(
            put("/api/v1/organizations/{id}/mailbox", organization.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(asUser(manager))
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRefuseInternalMailboxesWithoutTheKey() throws Exception {
        mockMvc.perform(get("/internal/v1/mailboxes"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldServeMailboxesWithPasswordToInternalCaller() throws Exception {
        Organization organization = organization();
        User manager = manager(organization);

        mockMvc.perform(connectRequest(organization, manager, PASSWORD))
            .andExpect(status().isOk());

        mockMvc.perform(
            get("/internal/v1/mailboxes").header("X-Internal-Token", "local-internal-key-change-me")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].organizationCode").value("megafaculty"))
            .andExpect(jsonPath("$[0].password").value(PASSWORD));
    }

    private MockHttpServletRequestBuilder connectRequest(
        Organization organization,
        User actor,
        String password
    ) throws Exception {
        ConnectMailboxRequest request = new ConnectMailboxRequest(
            "imap.mail.ru",
            993,
            "megafaculty@mail.ru",
            password,
            "INBOX",
            true
        );

        return put("/api/v1/organizations/{id}/mailbox", organization.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .with(asUser(actor));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor asUser(User user) {
        return jwt().jwt(token -> token.subject(String.valueOf(user.getId())));
    }

    private User manager(Organization organization) throws Exception {
        User admin = account(ADMIN_ISU);
        User manager = account(MANAGER_ISU);

        participant(organization, MANAGER_ISU, manager);

        mockMvc.perform(
            put("/api/v1/organizations/{id}/manager", organization.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new AssignOrganizationRoleRequest(MANAGER_ISU)
                ))
                .with(jwt()
                    .jwt(token -> token.subject(String.valueOf(admin.getId())))
                    .authorities(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN")))
        )
            .andExpect(status().isOk());

        return manager;
    }

    private Organization organization() {
        Organization organization = new Organization();

        organization.setCode("megafaculty");
        organization.setName("Мегафакультет");

        return organizationRepository.save(organization);
    }

    private OrganizationParticipant participant(
        Organization organization,
        String isuNumber,
        User user
    ) {
        OrganizationParticipant participant = new OrganizationParticipant();

        participant.setOrganization(organization);
        participant.setIsuNumber(isuNumber);
        participant.setLastName("Иванов");
        participant.setFirstName("Иван");
        participant.setEmail(isuNumber + "@niuitmo.ru");
        participant.setStatus(ParticipantStatus.ACTIVE);
        participant.setUser(user);

        return participantRepository.save(participant);
    }

    private User account(String isuNumber) {
        return userRepository.findByIsuNumber(isuNumber).orElseGet(() -> {
            User user = new User();

            user.setIsuNumber(isuNumber);
            user.setEmail(isuNumber + "@niuitmo.ru");
            user.setPasswordHash("hash");
            user.setLastName("Иванов");
            user.setFirstName("Иван");
            user.setStatus(UserStatus.ACTIVE);

            return userRepository.save(user);
        });
    }
}
