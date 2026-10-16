package ru.itmo.wmp.platform.api.organization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import ru.itmo.wmp.platform.IntegrationTest;
import ru.itmo.wmp.platform.domain.organization.Organization;
import ru.itmo.wmp.platform.domain.organization.OrganizationParticipant;
import ru.itmo.wmp.platform.domain.organization.ParticipantStatus;
import ru.itmo.wmp.platform.domain.user.User;
import ru.itmo.wmp.platform.domain.user.UserStatus;
import ru.itmo.wmp.platform.dto.organization.AssignOrganizationRoleRequest;
import ru.itmo.wmp.platform.infrastructure.persistance.organization.JpaOrganizationParticipantRepository;
import ru.itmo.wmp.platform.infrastructure.persistance.organization.JpaOrganizationRepository;
import ru.itmo.wmp.platform.infrastructure.persistance.user.JpaUserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
public class AuthorizationCacheIntegrationTest {
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
    private StringRedisTemplate redisTemplate;

    @Test
    void shouldCacheAuthorizationSnapshotAfterCheck() throws Exception {
        Organization organization = organization("megafaculty");
        User manager = account(MANAGER_ISU);

        mockMvc.perform(asPlatformAdmin(assignManager(organization.getId(), MANAGER_ISU)))
            .andExpect(status().isOk());

        mockMvc.perform(asUser(participants(organization.getId()), manager))
            .andExpect(status().isOk());

        assertThat(redisTemplate.keys("wmp:authorization:user:" + manager.getId() + ":*"))
            .isNotEmpty();
    }

    @Test
    void shouldForbidImmediatelyAfterAdminRoleRevoked() throws Exception {
        Organization organization = organization("megafaculty");
        User manager = account(MANAGER_ISU);
        User member = account(MEMBER_ISU);

        participant(organization, member, MEMBER_ISU);

        mockMvc.perform(asPlatformAdmin(assignManager(organization.getId(), MANAGER_ISU)))
            .andExpect(status().isOk());

        mockMvc.perform(asUser(grantAdmin(organization.getId(), MEMBER_ISU), manager))
            .andExpect(status().isOk());

        mockMvc.perform(asUser(participants(organization.getId()), member))
            .andExpect(status().isOk());

        mockMvc.perform(asUser(revokeAdmin(organization.getId(), MEMBER_ISU), manager))
            .andExpect(status().isNoContent());

        mockMvc.perform(asUser(participants(organization.getId()), member))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldForbidImmediatelyAfterParticipantExcluded() throws Exception {
        Organization organization = organization("megafaculty");
        User manager = account(MANAGER_ISU);
        User member = account(MEMBER_ISU);

        participant(organization, member, MEMBER_ISU);

        mockMvc.perform(asPlatformAdmin(assignManager(organization.getId(), MANAGER_ISU)))
            .andExpect(status().isOk());

        mockMvc.perform(asUser(organization(organization.getId()), member))
            .andExpect(status().isOk());

        mockMvc.perform(
            asUser(
                delete(
                    "/api/v1/organizations/{id}/participants/{isuNumber}",
                    organization.getId(),
                    MEMBER_ISU
                ),
                manager
            )
        )
            .andExpect(status().isOk());

        mockMvc.perform(asUser(organization(organization.getId()), member))
            .andExpect(status().isForbidden());
    }

    private MockHttpServletRequestBuilder assignManager(Long organizationId, String isuNumber)
        throws Exception {
        return put("/api/v1/organizations/{id}/manager", organizationId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new AssignOrganizationRoleRequest(isuNumber)));
    }

    private MockHttpServletRequestBuilder grantAdmin(Long organizationId, String isuNumber)
        throws Exception {
        return put("/api/v1/organizations/{id}/admins", organizationId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new AssignOrganizationRoleRequest(isuNumber)));
    }

    private MockHttpServletRequestBuilder revokeAdmin(Long organizationId, String isuNumber) {
        return delete("/api/v1/organizations/{id}/admins/{isuNumber}", organizationId, isuNumber);
    }

    private MockHttpServletRequestBuilder participants(Long organizationId) {
        return get("/api/v1/organizations/{id}/participants", organizationId);
    }

    private MockHttpServletRequestBuilder organization(Long organizationId) {
        return get("/api/v1/organizations/{id}", organizationId);
    }

    private MockHttpServletRequestBuilder asPlatformAdmin(MockHttpServletRequestBuilder builder) {
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

    private Organization organization(String code) {
        Organization organization = new Organization();

        organization.setCode(code);
        organization.setName("Мегафакультет");

        return organizationRepository.save(organization);
    }

    private OrganizationParticipant participant(
        Organization organization,
        User user,
        String isuNumber
    ) {
        OrganizationParticipant participant = new OrganizationParticipant();

        participant.setOrganization(organization);
        participant.setUser(user);
        participant.setIsuNumber(isuNumber);
        participant.setLastName("Иванов");
        participant.setFirstName("Иван");
        participant.setEmail(isuNumber + "@niuitmo.ru");
        participant.setStatus(ParticipantStatus.ACTIVE);

        return participantRepository.save(participant);
    }

    private User account(String isuNumber) {
        return userRepository.findByIsuNumber(isuNumber)
            .orElseGet(() -> {
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
