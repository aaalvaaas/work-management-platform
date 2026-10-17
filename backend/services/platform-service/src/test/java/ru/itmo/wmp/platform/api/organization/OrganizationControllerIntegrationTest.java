package ru.itmo.wmp.platform.api.organization;

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
import ru.itmo.wmp.platform.domain.organization.ParticipantStatus;
import ru.itmo.wmp.platform.domain.user.User;
import ru.itmo.wmp.platform.dto.organization.AssignOrganizationRoleRequest;
import ru.itmo.wmp.platform.dto.organization.CreateOrganizationRequest;
import ru.itmo.wmp.platform.dto.organization.UpdateOrganizationRequest;
import ru.itmo.wmp.platform.dto.organization.UpdateOrganizationStatusRequest;
import ru.itmo.wmp.platform.domain.organization.OrganizationStatus;
import ru.itmo.wmp.platform.infrastructure.persistance.organization.JpaOrganizationParticipantRepository;
import ru.itmo.wmp.platform.infrastructure.persistance.organization.JpaOrganizationRepository;
import ru.itmo.wmp.platform.infrastructure.persistance.user.JpaUserRepository;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@IntegrationTest
public class OrganizationControllerIntegrationTest {
    private static final String ADMIN_ISU = "100500";
    private static final String MANAGER_ISU = "465824";
    private static final String MEMBER_ISU = "465825";
    private static final String OUTSIDER_ISU = "465826";

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
    void shouldCreateOrganizationByPlatformAdmin() throws Exception {
        mockMvc.perform(asPlatformAdmin(createRequest("megafaculty", "Мегафакультет")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value("megafaculty"))
            .andExpect(jsonPath("$.name").value("Мегафакультет"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.managerIsuNumber").doesNotExist());
    }

    @Test
    void shouldRejectDuplicateCode() throws Exception {
        organization("megafaculty", "Мегафакультет");

        mockMvc.perform(asPlatformAdmin(createRequest("megafaculty", "Другая")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("ORGANIZATION_CODE_ALREADY_USED"));
    }

    @Test
    void shouldRejectInvalidCode() throws Exception {
        mockMvc.perform(asPlatformAdmin(createRequest("Мега Факультет", "Мегафакультет")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldForbidCreationForRegularUser() throws Exception {
        User user = account(MEMBER_ISU);

        mockMvc.perform(asUser(createRequest("megafaculty", "Мегафакультет"), user))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldAssignManagerAndShowHimInOrganization() throws Exception {
        Organization organization = organization("megafaculty", "Мегафакультет");
        account(MANAGER_ISU);

        mockMvc.perform(asPlatformAdmin(assignManagerRequest(organization.getId(), MANAGER_ISU)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isuNumber").value(MANAGER_ISU))
            .andExpect(jsonPath("$.role").value("MANAGER"));

        mockMvc.perform(asPlatformAdmin(get("/api/v1/organizations/{id}", organization.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.managerIsuNumber").value(MANAGER_ISU));
    }

    @Test
    void shouldReplacePreviousManager() throws Exception {
        Organization organization = organization("megafaculty", "Мегафакультет");
        account(MANAGER_ISU);
        account(MEMBER_ISU);

        mockMvc.perform(asPlatformAdmin(assignManagerRequest(organization.getId(), MANAGER_ISU)))
            .andExpect(status().isOk());

        mockMvc.perform(asPlatformAdmin(assignManagerRequest(organization.getId(), MEMBER_ISU)))
            .andExpect(status().isOk());

        mockMvc.perform(asPlatformAdmin(get("/api/v1/organizations/{id}", organization.getId())))
            .andExpect(jsonPath("$.managerIsuNumber").value(MEMBER_ISU));
    }

    @Test
    void shouldAllowManagerToRenameOrganization() throws Exception {
        Organization organization = organization("megafaculty", "Мегафакультет");
        User manager = account(MANAGER_ISU);

        mockMvc.perform(asPlatformAdmin(assignManagerRequest(organization.getId(), MANAGER_ISU)))
            .andExpect(status().isOk());

        mockMvc.perform(
            asUser(
                patch("/api/v1/organizations/{id}", organization.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                        new UpdateOrganizationRequest("Мегафакультет ИТМО")
                    )),
                manager
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Мегафакультет ИТМО"));
    }

    @Test
    void shouldForbidRenameForParticipantWithoutRole() throws Exception {
        Organization organization = organization("megafaculty", "Мегафакультет");
        User member = account(MEMBER_ISU);

        participant(organization, member, MEMBER_ISU);

        mockMvc.perform(
            asUser(
                patch("/api/v1/organizations/{id}", organization.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                        new UpdateOrganizationRequest("Своё название")
                    )),
                member
            )
        )
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldForbidStatusChangeForManager() throws Exception {
        Organization organization = organization("megafaculty", "Мегафакультет");
        User manager = account(MANAGER_ISU);

        mockMvc.perform(asPlatformAdmin(assignManagerRequest(organization.getId(), MANAGER_ISU)))
            .andExpect(status().isOk());

        mockMvc.perform(
            asUser(
                put("/api/v1/organizations/{id}/status", organization.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                        new UpdateOrganizationStatusRequest(OrganizationStatus.INACTIVE)
                    )),
                manager
            )
        )
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldGrantAdminOnlyToActiveParticipant() throws Exception {
        Organization organization = organization("megafaculty", "Мегафакультет");
        User manager = account(MANAGER_ISU);
        User member = account(MEMBER_ISU);
        User outsider = account(OUTSIDER_ISU);

        participant(organization, member, MEMBER_ISU);

        mockMvc.perform(asPlatformAdmin(assignManagerRequest(organization.getId(), MANAGER_ISU)))
            .andExpect(status().isOk());

        mockMvc.perform(asUser(grantAdminRequest(organization.getId(), MEMBER_ISU), manager))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("ADMIN"));

        mockMvc.perform(asUser(grantAdminRequest(organization.getId(), OUTSIDER_ISU), manager))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("NOT_ORGANIZATION_PARTICIPANT"));
    }

    @Test
    void shouldLetOrganizationAdminSeeParticipants() throws Exception {
        Organization organization = organization("megafaculty", "Мегафакультет");
        User manager = account(MANAGER_ISU);
        User member = account(MEMBER_ISU);

        participant(organization, member, MEMBER_ISU);

        mockMvc.perform(asPlatformAdmin(assignManagerRequest(organization.getId(), MANAGER_ISU)))
            .andExpect(status().isOk());

        mockMvc.perform(
            asUser(get("/api/v1/organizations/{id}/participants", organization.getId()), manager)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].isuNumber").value(MEMBER_ISU))
            .andExpect(jsonPath("$[0].registered").value(true));

        mockMvc.perform(
            asUser(get("/api/v1/organizations/{id}/participants", organization.getId()), member)
        )
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldListOnlyOwnOrganizationsForRegularUser() throws Exception {
        Organization own = organization("megafaculty", "Мегафакультет");
        organization("studsovet", "Студсовет");

        User member = account(MEMBER_ISU);

        participant(own, member, MEMBER_ISU);

        mockMvc.perform(asUser(get("/api/v1/organizations"), member))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].code").value("megafaculty"));
    }

    @Test
    void shouldHideForeignOrganizationFromNonMember() throws Exception {
        Organization organization = organization("megafaculty", "Мегафакультет");
        User outsider = account(OUTSIDER_ISU);

        mockMvc.perform(asUser(get("/api/v1/organizations/{id}", organization.getId()), outsider))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldReportMembershipOfTheManager() throws Exception {
        Organization organization = organization("megafaculty", "Мегафакультет");
        User manager = account(MANAGER_ISU);

        participant(organization, manager, MANAGER_ISU);

        mockMvc.perform(asPlatformAdmin(assignManagerRequest(organization.getId(), MANAGER_ISU)))
            .andExpect(status().isOk());

        mockMvc.perform(
            asUser(get("/api/v1/organizations/{id}/membership", organization.getId()), manager)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.member").value(true))
            .andExpect(jsonPath("$.platformAdmin").value(false))
            .andExpect(jsonPath("$.roles[0]").value("MANAGER"))
            .andExpect(jsonPath("$.userId").value(manager.getId()));
    }

    @Test
    void shouldReportEmptyMembershipForOutsider() throws Exception {
        Organization organization = organization("megafaculty", "Мегафакультет");
        User outsider = account(OUTSIDER_ISU);

        mockMvc.perform(
            asUser(get("/api/v1/organizations/{id}/membership", organization.getId()), outsider)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.member").value(false))
            .andExpect(jsonPath("$.roles").isEmpty())
            .andExpect(jsonPath("$.blocks").isEmpty());
    }

    private MockHttpServletRequestBuilder createRequest(String code, String name) throws Exception {
        return post("/api/v1/organizations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new CreateOrganizationRequest(code, name)));
    }

    private MockHttpServletRequestBuilder assignManagerRequest(
        Long organizationId,
        String isuNumber
    ) throws Exception {
        return put("/api/v1/organizations/{id}/manager", organizationId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new AssignOrganizationRoleRequest(isuNumber)));
    }

    private MockHttpServletRequestBuilder grantAdminRequest(
        Long organizationId,
        String isuNumber
    ) throws Exception {
        return put("/api/v1/organizations/{id}/admins", organizationId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new AssignOrganizationRoleRequest(isuNumber)));
    }

    private MockHttpServletRequestBuilder asPlatformAdmin(
        MockHttpServletRequestBuilder builder
    ) {
        User admin = account(ADMIN_ISU);

        return builder.with(
            jwt()
                .jwt(token -> token.subject(String.valueOf(admin.getId())))
                .authorities(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"))
        );
    }

    private MockHttpServletRequestBuilder asUser(
        MockHttpServletRequestBuilder builder,
        User user
    ) {
        return builder.with(jwt().jwt(token -> token.subject(String.valueOf(user.getId()))));
    }

    private Organization organization(String code, String name) {
        Organization organization = new Organization();

        organization.setCode(code);
        organization.setName(name);

        return organizationRepository.save(organization);
    }

    private OrganizationParticipant participant(
        Organization organization,
        User user,
        String isuNumber
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

            return userRepository.save(user);
        });
    }
}
