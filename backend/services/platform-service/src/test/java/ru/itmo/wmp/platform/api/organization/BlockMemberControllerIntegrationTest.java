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
import ru.itmo.wmp.platform.dto.organization.AssignBlockRoleRequest;
import ru.itmo.wmp.platform.dto.organization.AssignOrganizationRoleRequest;
import ru.itmo.wmp.platform.infrastructure.persistance.organization.JpaBlockRepository;
import ru.itmo.wmp.platform.infrastructure.persistance.organization.JpaOrganizationParticipantRepository;
import ru.itmo.wmp.platform.infrastructure.persistance.organization.JpaOrganizationRepository;
import ru.itmo.wmp.platform.infrastructure.persistance.user.JpaUserRepository;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@IntegrationTest
public class BlockMemberControllerIntegrationTest {
    private static final String ADMIN_ISU = "100500";
    private static final String MANAGER_ISU = "465824";
    private static final String MEMBER_ISU = "465825";
    private static final String OUTSIDER_ISU = "465826";
    private static final String BLOCK_LEAD_ISU = "465827";

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
    void shouldAddParticipantToBlockByOrganizationManager() throws Exception {
        Organization organization = organization("megafaculty");
        User manager = organizationManager(organization);
        Block block = block(organization, "Разработка");

        linkUser(organization, MEMBER_ISU, account(MEMBER_ISU));

        mockMvc.perform(asUser(addMemberRequest(organization.getId(), block.getId(), MEMBER_ISU), manager))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.isuNumber").value(MEMBER_ISU))
            .andExpect(jsonPath("$.registered").value(true))
            .andExpect(jsonPath("$.addedAt").exists());
    }

    @Test
    void shouldAddUnregisteredParticipantToBlock() throws Exception {
        Organization organization = organization("megafaculty");
        User manager = organizationManager(organization);
        Block block = block(organization, "Разработка");

        participant(organization, MEMBER_ISU, ParticipantStatus.ACTIVE);

        mockMvc.perform(asUser(addMemberRequest(organization.getId(), block.getId(), MEMBER_ISU), manager))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.registered").value(false));
    }

    @Test
    void shouldBeIdempotentOnRepeatedAdd() throws Exception {
        Organization organization = organization("megafaculty");
        User manager = organizationManager(organization);
        Block block = block(organization, "Разработка");

        participant(organization, MEMBER_ISU, ParticipantStatus.ACTIVE);

        mockMvc.perform(asUser(addMemberRequest(organization.getId(), block.getId(), MEMBER_ISU), manager))
            .andExpect(status().isCreated());

        mockMvc.perform(asUser(addMemberRequest(organization.getId(), block.getId(), MEMBER_ISU), manager))
            .andExpect(status().isCreated());

        mockMvc.perform(asUser(membersRequest(organization.getId(), block.getId()), manager))
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void shouldRejectParticipantFromAnotherOrganization() throws Exception {
        Organization organization = organization("megafaculty");
        Organization another = organization("studsovet");

        User manager = organizationManager(organization);
        Block block = block(organization, "Разработка");

        participant(another, OUTSIDER_ISU, ParticipantStatus.ACTIVE);

        mockMvc.perform(asUser(addMemberRequest(organization.getId(), block.getId(), OUTSIDER_ISU), manager))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("PARTICIPANT_NOT_FOUND"));
    }

    @Test
    void shouldRejectExcludedParticipant() throws Exception {
        Organization organization = organization("megafaculty");
        User manager = organizationManager(organization);
        Block block = block(organization, "Разработка");

        participant(organization, MEMBER_ISU, ParticipantStatus.INACTIVE);

        mockMvc.perform(asUser(addMemberRequest(organization.getId(), block.getId(), MEMBER_ISU), manager))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("NOT_ORGANIZATION_PARTICIPANT"));
    }

    @Test
    void shouldAllowBlockManagerToManageOwnBlockOnly() throws Exception {
        Organization organization = organization("megafaculty");
        User manager = organizationManager(organization);

        Block own = block(organization, "Разработка");
        Block foreign = block(organization, "Дизайн");

        User blockLead = account(BLOCK_LEAD_ISU);

        participant(organization, MEMBER_ISU, ParticipantStatus.ACTIVE);

        linkUser(organization, BLOCK_LEAD_ISU, blockLead);

        mockMvc.perform(
            asUser(
                put("/api/v1/organizations/{id}/blocks/{blockId}/manager", organization.getId(), own.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new AssignBlockRoleRequest(BLOCK_LEAD_ISU))),
                manager
            )
        )
            .andExpect(status().isOk());

        mockMvc.perform(asUser(addMemberRequest(organization.getId(), own.getId(), MEMBER_ISU), blockLead))
            .andExpect(status().isCreated());

        mockMvc.perform(asUser(addMemberRequest(organization.getId(), foreign.getId(), MEMBER_ISU), blockLead))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldForbidMemberManagementForRegularParticipant() throws Exception {
        Organization organization = organization("megafaculty");
        organizationManager(organization);

        Block block = block(organization, "Разработка");

        User member = account(MEMBER_ISU);

        linkUser(organization, MEMBER_ISU, member);

        mockMvc.perform(asUser(addMemberRequest(organization.getId(), block.getId(), MEMBER_ISU), member))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldRemoveParticipantFromBlock() throws Exception {
        Organization organization = organization("megafaculty");
        User manager = organizationManager(organization);
        Block block = block(organization, "Разработка");

        participant(organization, MEMBER_ISU, ParticipantStatus.ACTIVE);

        mockMvc.perform(asUser(addMemberRequest(organization.getId(), block.getId(), MEMBER_ISU), manager))
            .andExpect(status().isCreated());

        mockMvc.perform(
            asUser(
                delete(
                    "/api/v1/organizations/{id}/blocks/{blockId}/members/{isu}",
                    organization.getId(),
                    block.getId(),
                    MEMBER_ISU
                ),
                manager
            )
        )
            .andExpect(status().isNoContent());

        mockMvc.perform(asUser(membersRequest(organization.getId(), block.getId()), manager))
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldHideBlockMembersFromOutsider() throws Exception {
        Organization organization = organization("megafaculty");
        organizationManager(organization);

        Block block = block(organization, "Разработка");

        User outsider = account(OUTSIDER_ISU);

        mockMvc.perform(asUser(membersRequest(organization.getId(), block.getId()), outsider))
            .andExpect(status().isForbidden());
    }

    private MockHttpServletRequestBuilder addMemberRequest(
        Long organizationId,
        Long blockId,
        String isuNumber
    ) throws Exception {
        return post("/api/v1/organizations/{id}/blocks/{blockId}/members", organizationId, blockId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new AddBlockMemberRequest(isuNumber)));
    }

    private MockHttpServletRequestBuilder membersRequest(Long organizationId, Long blockId) {
        return get("/api/v1/organizations/{id}/blocks/{blockId}/members", organizationId, blockId);
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
            .orElseGet(() -> participant(organization, isuNumber, ParticipantStatus.ACTIVE));

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

    private OrganizationParticipant participant(
        Organization organization,
        String isuNumber,
        ParticipantStatus status
    ) {
        OrganizationParticipant participant = new OrganizationParticipant();

        participant.setOrganization(organization);
        participant.setIsuNumber(isuNumber);
        participant.setLastName("Иванов");
        participant.setFirstName("Иван");
        participant.setEmail(isuNumber + "@niuitmo.ru");
        participant.setStatus(status);

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
