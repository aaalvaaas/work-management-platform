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
import ru.itmo.wmp.platform.dto.organization.AssignBlockRoleRequest;
import ru.itmo.wmp.platform.dto.organization.AssignOrganizationRoleRequest;
import ru.itmo.wmp.platform.dto.organization.CreateBlockRequest;
import ru.itmo.wmp.platform.dto.organization.UpdateBlockRequest;
import ru.itmo.wmp.platform.infrastructure.persistance.organization.JpaBlockRepository;
import ru.itmo.wmp.platform.infrastructure.persistance.organization.JpaOrganizationParticipantRepository;
import ru.itmo.wmp.platform.infrastructure.persistance.organization.JpaOrganizationRepository;
import ru.itmo.wmp.platform.infrastructure.persistance.user.JpaUserRepository;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@IntegrationTest
public class BlockControllerIntegrationTest {
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

    @Autowired
    private JpaBlockRepository blockRepository;

    @Test
    void shouldCreateBlockByOrganizationManager() throws Exception {
        Organization organization = organization("megafaculty");
        User manager = manager(organization);

        mockMvc.perform(asUser(createBlockRequest(organization.getId(), "Разработка"), manager))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Разработка"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.organizationId").value(organization.getId()))
            .andExpect(jsonPath("$.managerIsuNumber").doesNotExist());
    }

    @Test
    void shouldRejectDuplicateBlockName() throws Exception {
        Organization organization = organization("megafaculty");
        User manager = manager(organization);

        block(organization, "Разработка");

        mockMvc.perform(asUser(createBlockRequest(organization.getId(), "Разработка"), manager))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("BLOCK_NAME_ALREADY_USED"));
    }

    @Test
    void shouldForbidBlockCreationForParticipant() throws Exception {
        Organization organization = organization("megafaculty");
        User member = account(MEMBER_ISU);

        participant(organization, member, MEMBER_ISU);

        mockMvc.perform(asUser(createBlockRequest(organization.getId(), "Разработка"), member))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldListBlocksForParticipantAndHideFromOutsider() throws Exception {
        Organization organization = organization("megafaculty");
        User member = account(MEMBER_ISU);
        User outsider = account(OUTSIDER_ISU);

        participant(organization, member, MEMBER_ISU);
        block(organization, "Разработка");

        mockMvc.perform(asUser(get("/api/v1/organizations/{id}/blocks", organization.getId()), member))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("Разработка"));

        mockMvc.perform(asUser(get("/api/v1/organizations/{id}/blocks", organization.getId()), outsider))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldRenameBlockAndChangeStatus() throws Exception {
        Organization organization = organization("megafaculty");
        User manager = manager(organization);
        Block block = block(organization, "Разработка");

        mockMvc.perform(
            asUser(
                patch("/api/v1/organizations/{id}/blocks/{blockId}", organization.getId(), block.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                        new UpdateBlockRequest("Разработка и тестирование", BlockStatus.INACTIVE)
                    )),
                manager
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Разработка и тестирование"))
            .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void shouldNotFindBlockFromAnotherOrganization() throws Exception {
        Organization organization = organization("megafaculty");
        Organization another = organization("studsovet");

        User manager = manager(organization);
        Block foreign = block(another, "Чужой блок");

        mockMvc.perform(
            asUser(
                get("/api/v1/organizations/{id}/blocks/{blockId}", organization.getId(), foreign.getId()),
                manager
            )
        )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("BLOCK_NOT_FOUND"));
    }

    @Test
    void shouldAssignBlockManagerOnlyFromOrganizationParticipants() throws Exception {
        Organization organization = organization("megafaculty");
        User manager = manager(organization);
        User member = account(MEMBER_ISU);
        User outsider = account(OUTSIDER_ISU);

        participant(organization, member, MEMBER_ISU);

        Block block = block(organization, "Разработка");

        mockMvc.perform(
            asUser(assignBlockManagerRequest(organization.getId(), block.getId(), MEMBER_ISU), manager)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isuNumber").value(MEMBER_ISU))
            .andExpect(jsonPath("$.role").value("MANAGER"));

        mockMvc.perform(
            asUser(assignBlockManagerRequest(organization.getId(), block.getId(), OUTSIDER_ISU), manager)
        )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("NOT_ORGANIZATION_PARTICIPANT"));

        mockMvc.perform(
            asUser(get("/api/v1/organizations/{id}/blocks/{blockId}", organization.getId(), block.getId()), manager)
        )
            .andExpect(jsonPath("$.managerIsuNumber").value(MEMBER_ISU));
    }

    private MockHttpServletRequestBuilder createBlockRequest(
        Long organizationId,
        String name
    ) throws Exception {
        return post("/api/v1/organizations/{id}/blocks", organizationId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new CreateBlockRequest(name)));
    }

    private MockHttpServletRequestBuilder assignBlockManagerRequest(
        Long organizationId,
        Long blockId,
        String isuNumber
    ) throws Exception {
        return put("/api/v1/organizations/{id}/blocks/{blockId}/manager", organizationId, blockId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new AssignBlockRoleRequest(isuNumber)));
    }

    private User manager(Organization organization) throws Exception {
        User admin = account(ADMIN_ISU);
        User manager = account(MANAGER_ISU);

        participant(organization, manager, MANAGER_ISU);

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
