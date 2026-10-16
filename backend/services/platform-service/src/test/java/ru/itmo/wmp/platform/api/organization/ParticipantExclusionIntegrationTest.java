package ru.itmo.wmp.platform.api.organization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import ru.itmo.wmp.platform.IntegrationTest;
import ru.itmo.wmp.platform.domain.organization.*;
import ru.itmo.wmp.platform.domain.user.User;
import ru.itmo.wmp.platform.dto.organization.AddBlockMemberRequest;
import ru.itmo.wmp.platform.dto.organization.AssignOrganizationRoleRequest;
import ru.itmo.wmp.platform.infrastructure.persistance.organization.JpaBlockRepository;
import ru.itmo.wmp.platform.infrastructure.persistance.organization.JpaOrganizationParticipantRepository;
import ru.itmo.wmp.platform.infrastructure.persistance.organization.JpaOrganizationRepository;
import ru.itmo.wmp.platform.infrastructure.persistance.user.JpaUserRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@IntegrationTest
public class ParticipantExclusionIntegrationTest {
    private static final String ADMIN_ISU = "100500";
    private static final String MANAGER_ISU = "465824";
    private static final String MEMBER_ISU = "465825";

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
    private JpaBlockRepository blockRepository;

    @Test
    void shouldExcludeParticipantAndClearBlockMembership() throws Exception {
        Organization organization = organization("megafaculty");
        User manager = organizationManager(organization);
        Block block = block(organization, "Разработка");

        linkUser(organization, MEMBER_ISU, account(MEMBER_ISU));

        mockMvc.perform(
            asUser(
                post("/api/v1/organizations/{id}/blocks/{blockId}/members", organization.getId(), block.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new AddBlockMemberRequest(MEMBER_ISU))),
                manager
            )
        )
            .andExpect(status().isCreated());

        mockMvc.perform(asUser(excludeRequest(organization.getId(), MEMBER_ISU), manager))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isuNumber").value(MEMBER_ISU))
            .andExpect(jsonPath("$.status").value("INACTIVE"));

        mockMvc.perform(
            asUser(
                get("/api/v1/organizations/{id}/blocks/{blockId}/members", organization.getId(), block.getId()),
                manager
            )
        )
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldKeepAccountAndOtherOrganizationsAfterExclusion() throws Exception {
        Organization organization = organization("megafaculty");
        Organization another = organization("studsovet");

        User manager = organizationManager(organization);
        User member = account(MEMBER_ISU);

        linkUser(organization, MEMBER_ISU, member);
        linkUser(another, MEMBER_ISU, member);

        mockMvc.perform(asUser(excludeRequest(organization.getId(), MEMBER_ISU), manager))
            .andExpect(status().isOk());

        assertEquals(
            ParticipantStatus.ACTIVE,
            participantRepository
                .findByOrganizationIdAndIsuNumber(another.getId(), MEMBER_ISU)
                .orElseThrow()
                .getStatus()
        );

        mockMvc.perform(
            get("/api/v1/users/me")
                .with(jwt().jwt(token -> token.subject(String.valueOf(member.getId()))))
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void shouldRevokeOrganizationAdminRoleOnExclusion() throws Exception {
        Organization organization = organization("megafaculty");
        User manager = organizationManager(organization);
        User member = account(MEMBER_ISU);

        linkUser(organization, MEMBER_ISU, member);

        mockMvc.perform(
            asUser(
                put("/api/v1/organizations/{id}/admins", organization.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                        new AssignOrganizationRoleRequest(MEMBER_ISU)
                    )),
                manager
            )
        )
            .andExpect(status().isOk());

        mockMvc.perform(asUser(excludeRequest(organization.getId(), MEMBER_ISU), manager))
            .andExpect(status().isOk());

        mockMvc.perform(
            asUser(get("/api/v1/organizations/{id}/participants", organization.getId()), member)
        )
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn404ForUnknownParticipant() throws Exception {
        Organization organization = organization("megafaculty");
        User manager = organizationManager(organization);

        mockMvc.perform(asUser(excludeRequest(organization.getId(), "465999"), manager))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("PARTICIPANT_NOT_FOUND"));
    }

    @Test
    void shouldForbidExclusionForRegularParticipant() throws Exception {
        Organization organization = organization("megafaculty");
        organizationManager(organization);

        User member = account(MEMBER_ISU);

        linkUser(organization, MEMBER_ISU, member);

        mockMvc.perform(asUser(excludeRequest(organization.getId(), MANAGER_ISU), member))
            .andExpect(status().isForbidden());
    }

    private MockHttpServletRequestBuilder excludeRequest(Long organizationId, String isuNumber) {
        return delete(
            "/api/v1/organizations/{id}/participants/{isu}",
            organizationId,
            isuNumber
        );
    }

    private User organizationManager(Organization organization) throws Exception {
        User admin = account(ADMIN_ISU);
        User manager = account(MANAGER_ISU);

        linkUser(organization, MANAGER_ISU, manager);

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

    private OrganizationParticipant linkUser(Organization organization, String isuNumber, User user) {
        OrganizationParticipant participant = participantRepository
            .findByOrganizationIdAndIsuNumber(organization.getId(), isuNumber)
            .orElseGet(() -> {
                OrganizationParticipant created = new OrganizationParticipant();

                created.setOrganization(organization);
                created.setIsuNumber(isuNumber);
                created.setLastName("Иванов");
                created.setFirstName("Иван");
                created.setEmail(isuNumber + "@niuitmo.ru");
                created.setStatus(ParticipantStatus.ACTIVE);

                return created;
            });

        participant.setUser(user);

        return participantRepository.save(participant);
    }

    private MockHttpServletRequestBuilder asUser(
        MockHttpServletRequestBuilder builder,
        User user
    ) {
        return builder.with(jwt().jwt(token -> token.subject(String.valueOf(user.getId()))));
    }

    private Organization organization(String code) {
        Organization organization = new Organization();

        organization.setCode(code);
        organization.setName(code);

        return organizationRepository.save(organization);
    }

    private Block block(Organization organization, String name) {
        Block block = new Block();

        block.setOrganization(organization);
        block.setName(name);

        return blockRepository.save(block);
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
