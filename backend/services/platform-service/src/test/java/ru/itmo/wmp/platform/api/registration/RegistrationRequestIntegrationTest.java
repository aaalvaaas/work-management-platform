package ru.itmo.wmp.platform.api.registration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import ru.itmo.wmp.platform.IntegrationTest;
import ru.itmo.wmp.platform.domain.organization.Organization;
import ru.itmo.wmp.platform.domain.organization.OrganizationParticipant;
import ru.itmo.wmp.platform.domain.organization.OrganizationStatus;
import ru.itmo.wmp.platform.domain.organization.ParticipantStatus;
import ru.itmo.wmp.platform.domain.user.User;
import ru.itmo.wmp.platform.domain.user.UserStatus;
import ru.itmo.wmp.platform.dto.auth.LoginRequest;
import ru.itmo.wmp.platform.dto.organization.AssignOrganizationRoleRequest;
import ru.itmo.wmp.platform.dto.registration.RejectRegistrationRequest;
import ru.itmo.wmp.platform.dto.registration.SubmitRegistrationRequest;
import ru.itmo.wmp.platform.infrastructure.persistance.organization.JpaOrganizationParticipantRepository;
import ru.itmo.wmp.platform.infrastructure.persistance.organization.JpaOrganizationRepository;
import ru.itmo.wmp.platform.infrastructure.persistance.user.JpaUserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
public class RegistrationRequestIntegrationTest {
    private static final String ADMIN_ISU = "100500";
    private static final String MANAGER_ISU = "465824";
    private static final String APPLICANT_ISU = "465830";
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

    @Test
    void shouldAcceptRequestAndCreateNoAccountYet() throws Exception {
        organization("megafaculty");

        mockMvc.perform(submit("megafaculty", APPLICANT_ISU))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.isuNumber").value(APPLICANT_ISU))
            .andExpect(jsonPath("$.organizationName").value("megafaculty"));

        assertThat(userRepository.findByIsuNumber(APPLICANT_ISU)).isEmpty();
    }

    @Test
    void shouldRejectUnknownOrganizationCode() throws Exception {
        mockMvc.perform(submit("unknown-org", APPLICANT_ISU))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("ORGANIZATION_NOT_FOUND"));
    }

    @Test
    void shouldRejectInactiveOrganization() throws Exception {
        Organization organization = organization("megafaculty");

        organization.setStatus(OrganizationStatus.INACTIVE);
        organizationRepository.save(organization);

        mockMvc.perform(submit("megafaculty", APPLICANT_ISU))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("REGISTRATION_NOT_ALLOWED"));
    }

    @Test
    void shouldRejectSecondRequestWhileTheFirstIsWaiting() throws Exception {
        organization("megafaculty");

        mockMvc.perform(submit("megafaculty", APPLICANT_ISU))
            .andExpect(status().isCreated());

        mockMvc.perform(submit("megafaculty", APPLICANT_ISU))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("REGISTRATION_REQUEST_ALREADY_SUBMITTED"));
    }

    @Test
    void shouldRejectRequestForRegisteredIsuNumber() throws Exception {
        organization("megafaculty");
        account(APPLICANT_ISU);

        mockMvc.perform(submit("megafaculty", APPLICANT_ISU))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("ALREADY_REGISTERED"));
    }

    @Test
    void shouldValidateSubmission() throws Exception {
        organization("megafaculty");

        SubmitRegistrationRequest request = new SubmitRegistrationRequest(
            "megafaculty",
            "123",
            "",
            "Пётр",
            null,
            "not-an-email",
            null,
            "short"
        );

        mockMvc.perform(
            post("/api/v1/registration/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldApproveRequestAndLetThePersonLogIn() throws Exception {
        Organization organization = organization("megafaculty");
        User manager = manager(organization);

        Long requestId = submitAndGetId(organization, APPLICANT_ISU);

        mockMvc.perform(decision(organization, requestId, "approve", manager))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"));

        User created = userRepository.findByIsuNumber(APPLICANT_ISU).orElseThrow();

        assertThat(created.getEmail()).isEqualTo(APPLICANT_ISU + "@niuitmo.ru");
        assertThat(created.getStatus()).isEqualTo(UserStatus.ACTIVE);

        OrganizationParticipant participant = participantRepository
            .findByOrganizationIdAndIsuNumber(organization.getId(), APPLICANT_ISU)
            .orElseThrow();

        assertThat(participant.getStatus()).isEqualTo(ParticipantStatus.ACTIVE);
        assertThat(participant.getUser().getId()).isEqualTo(created.getId());

        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new LoginRequest(APPLICANT_ISU, PASSWORD)
                ))
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    void shouldLinkTheExistingParticipantRowOnApproval() throws Exception {
        Organization organization = organization("megafaculty");
        User manager = manager(organization);

        OrganizationParticipant imported = participant(organization, APPLICANT_ISU, null);

        Long requestId = submitAndGetId(organization, APPLICANT_ISU);

        mockMvc.perform(decision(organization, requestId, "approve", manager))
            .andExpect(status().isOk());

        assertThat(participantRepository.findByOrganizationId(organization.getId()))
            .hasSize(2)
            .anyMatch(participant -> participant.getId().equals(imported.getId())
                && participant.getUser() != null);
    }

    @Test
    void shouldRejectRequestWithComment() throws Exception {
        Organization organization = organization("megafaculty");
        User manager = manager(organization);

        Long requestId = submitAndGetId(organization, APPLICANT_ISU);

        mockMvc.perform(
            decision(organization, requestId, "reject", manager)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new RejectRegistrationRequest("Нет такого человека в организации")
                ))
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REJECTED"))
            .andExpect(jsonPath("$.decisionComment").value("Нет такого человека в организации"));

        assertThat(userRepository.findByIsuNumber(APPLICANT_ISU)).isEmpty();
    }

    @Test
    void shouldNotDecideTheSameRequestTwice() throws Exception {
        Organization organization = organization("megafaculty");
        User manager = manager(organization);

        Long requestId = submitAndGetId(organization, APPLICANT_ISU);

        mockMvc.perform(decision(organization, requestId, "approve", manager))
            .andExpect(status().isOk());

        mockMvc.perform(decision(organization, requestId, "approve", manager))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("REGISTRATION_REQUEST_NOT_PENDING"));
    }

    @Test
    void shouldListPendingRequestsForOrganizationAdmin() throws Exception {
        Organization organization = organization("megafaculty");
        User manager = manager(organization);

        submitAndGetId(organization, APPLICANT_ISU);

        mockMvc.perform(
            get("/api/v1/organizations/{id}/registration-requests", organization.getId())
                .param("status", "PENDING")
                .with(asUser(manager))
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].isuNumber").value(APPLICANT_ISU));
    }

    @Test
    void shouldForbidRequestsForRegularParticipant() throws Exception {
        Organization organization = organization("megafaculty");
        User member = account("465900");

        participant(organization, "465900", member);

        mockMvc.perform(
            get("/api/v1/organizations/{id}/registration-requests", organization.getId())
                .with(asUser(member))
        )
            .andExpect(status().isForbidden());
    }

    private Long submitAndGetId(Organization organization, String isuNumber) throws Exception {
        String response = mockMvc.perform(submit(organization.getCode(), isuNumber))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    private MockHttpServletRequestBuilder submit(String code, String isuNumber) throws Exception {
        SubmitRegistrationRequest request = new SubmitRegistrationRequest(
            code,
            isuNumber,
            "Петров",
            "Пётр",
            "Петрович",
            isuNumber + "@niuitmo.ru",
            "студент",
            PASSWORD
        );

        return post("/api/v1/registration/requests")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request));
    }

    private MockHttpServletRequestBuilder decision(
        Organization organization,
        Long requestId,
        String action,
        User actor
    ) {
        return post(
            "/api/v1/organizations/{id}/registration-requests/{requestId}/" + action,
            organization.getId(),
            requestId
        ).with(asUser(actor));
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

    private Organization organization(String code) {
        Organization organization = new Organization();

        organization.setCode(code);
        organization.setName(code);
        organization.setStatus(OrganizationStatus.ACTIVE);

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
