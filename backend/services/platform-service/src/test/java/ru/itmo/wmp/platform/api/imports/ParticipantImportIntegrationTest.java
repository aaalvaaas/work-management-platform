package ru.itmo.wmp.platform.api.imports;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import ru.itmo.wmp.platform.IntegrationTest;
import ru.itmo.wmp.platform.domain.organization.*;
import ru.itmo.wmp.platform.domain.user.User;
import ru.itmo.wmp.platform.dto.organization.AssignOrganizationRoleRequest;
import ru.itmo.wmp.platform.infrastructure.persistance.organization.JpaBlockRepository;
import ru.itmo.wmp.platform.infrastructure.persistance.organization.JpaOrganizationParticipantRepository;
import ru.itmo.wmp.platform.infrastructure.persistance.organization.JpaOrganizationRepository;
import ru.itmo.wmp.platform.infrastructure.persistance.user.JpaUserRepository;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@IntegrationTest
public class ParticipantImportIntegrationTest {
    private static final String ADMIN_ISU = "100500";
    private static final String MANAGER_ISU = "465824";

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
    void shouldPreviewNewParticipantsWithoutChangingAnything() throws Exception {
        Organization organization = organization("megafaculty");
        User manager = manager(organization);

        block(organization, "Разработка");

        String csv = """
            Номер ИСУ,Фамилия,Имя,Email,Блоки
            465830,Петров,Пётр,petrov@niuitmo.ru,Разработка
            465831,Сидоров,Сидор,sidorov@niuitmo.ru,
            """;

        mockMvc.perform(upload(organization.getId(), csv, manager))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.batch.status").value("VALIDATED"))
            .andExpect(jsonPath("$.batch.totalRows").value(2))
            .andExpect(jsonPath("$.batch.created").value(2))
            .andExpect(jsonPath("$.batch.errors").value(0))
            .andExpect(jsonPath("$.changes[0].type").value("CREATE"));

        assertEquals(1, participantRepository.findByOrganizationId(organization.getId()).size());
    }

    @Test
    void shouldReportRowErrorsAndRefuseToApply() throws Exception {
        Organization organization = organization("megafaculty");
        User manager = manager(organization);

        String csv = """
            Номер ИСУ,Фамилия,Имя,Email,Блоки
            46583,Петров,Пётр,petrov@niuitmo.ru,
            465831,,Сидор,sidorov@niuitmo.ru,
            465832,Кузнецов,Кузьма,не-почта,
            465833,Смирнов,Семён,smirnov@niuitmo.ru,Несуществующий
            465830,Петров,Пётр,petrov@niuitmo.ru,
            465830,Петров,Пётр,petrov@niuitmo.ru,
            """;

        String response = mockMvc.perform(upload(organization.getId(), csv, manager))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.batch.status").value("FAILED"))
            .andExpect(jsonPath("$.batch.errors").value(5))
            .andReturn()
            .getResponse()
            .getContentAsString();

        long batchId = objectMapper.readTree(response).get("batch").get("id").asLong();

        mockMvc.perform(
            asUser(post("/api/v1/organizations/{id}/imports/{batchId}/apply", organization.getId(), batchId), manager)
        )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("IMPORT_NOT_APPLICABLE"));
    }

    @Test
    void shouldApplyImportAndCreateParticipantsWithBlocks() throws Exception {
        Organization organization = organization("megafaculty");
        User manager = manager(organization);
        Block block = block(organization, "Разработка");

        String csv = """
            Номер ИСУ,Фамилия,Имя,Отчество,Email,Должность,Блоки
            465830,Петров,Пётр,Петрович,petrov@niuitmo.ru,Аналитик,Разработка
            """;

        long batchId = uploadAndGetId(organization.getId(), csv, manager);

        mockMvc.perform(
            asUser(post("/api/v1/organizations/{id}/imports/{batchId}/apply", organization.getId(), batchId), manager)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPLIED"))
            .andExpect(jsonPath("$.appliedAt").exists());

        OrganizationParticipant created = participantRepository
            .findByOrganizationIdAndIsuNumber(organization.getId(), "465830")
            .orElseThrow();

        assertEquals("Петров", created.getLastName());
        assertEquals("Петрович", created.getPatronymic());
        assertEquals("Аналитик", created.getPosition());
        assertEquals(ParticipantStatus.ACTIVE, created.getStatus());

        mockMvc.perform(
            asUser(get("/api/v1/organizations/{id}/blocks/{blockId}/members", organization.getId(), block.getId()), manager)
        )
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].isuNumber").value("465830"));
    }

    @Test
    void shouldDetectUpdatesAndBlockChangesOnSecondUpload() throws Exception {
        Organization organization = organization("megafaculty");
        User manager = manager(organization);

        block(organization, "Разработка");
        block(organization, "Дизайн");

        String first = """
            Номер ИСУ,Фамилия,Имя,Email,Должность,Блоки
            465830,Петров,Пётр,petrov@niuitmo.ru,Аналитик,Разработка
            """;

        applyImport(organization.getId(), first, manager);

        String second = """
            Номер ИСУ,Фамилия,Имя,Email,Должность,Блоки
            465830,Петров,Пётр,petrov@itmo.ru,Ведущий аналитик,Дизайн
            """;

        mockMvc.perform(upload(organization.getId(), second, manager))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.batch.updated").value(1))
            .andExpect(jsonPath("$.changes[0].type").value("UPDATE"))
            .andExpect(jsonPath("$.changes[0].details").value(
                org.hamcrest.Matchers.allOf(
                    org.hamcrest.Matchers.containsString("email"),
                    org.hamcrest.Matchers.containsString("блок +дизайн"),
                    org.hamcrest.Matchers.containsString("блок -разработка")
                )
            ));
    }

    @Test
    void shouldDeactivateParticipantsMissingFromFile() throws Exception {
        Organization organization = organization("megafaculty");
        User manager = manager(organization);

        String first = """
            Номер ИСУ,Фамилия,Имя,Email
            465830,Петров,Пётр,petrov@niuitmo.ru
            465831,Сидоров,Сидор,sidorov@niuitmo.ru
            """;

        applyImport(organization.getId(), first, manager);

        String second = """
            Номер ИСУ,Фамилия,Имя,Email
            465830,Петров,Пётр,petrov@niuitmo.ru
            """;

        long batchId = uploadAndGetId(organization.getId(), second, manager);

        mockMvc.perform(
            asUser(get("/api/v1/organizations/{id}/imports/{batchId}", organization.getId(), batchId), manager)
        )
            .andExpect(jsonPath("$.batch.deactivated").value(1))
            .andExpect(jsonPath("$.changes[?(@.type == 'DEACTIVATE')].isuNumber").value(
                org.hamcrest.Matchers.hasItem("465831")
            ));

        mockMvc.perform(
            asUser(post("/api/v1/organizations/{id}/imports/{batchId}/apply", organization.getId(), batchId), manager)
        )
            .andExpect(status().isOk());

        assertEquals(
            ParticipantStatus.INACTIVE,
            participantRepository
                .findByOrganizationIdAndIsuNumber(organization.getId(), "465831")
                .orElseThrow()
                .getStatus()
        );

        assertEquals(
            ParticipantStatus.ACTIVE,
            participantRepository
                .findByOrganizationIdAndIsuNumber(organization.getId(), "465830")
                .orElseThrow()
                .getStatus()
        );
    }

    @Test
    void shouldLinkImportedParticipantToExistingAccount() throws Exception {
        Organization organization = organization("megafaculty");
        User manager = manager(organization);

        User registered = account("465830");

        String csv = """
            Номер ИСУ,Фамилия,Имя,Email
            465830,Петров,Пётр,petrov@niuitmo.ru
            """;

        applyImport(organization.getId(), csv, manager);

        assertEquals(
            registered.getId(),
            participantRepository
                .findByOrganizationIdAndIsuNumber(organization.getId(), "465830")
                .orElseThrow()
                .getUser()
                .getId()
        );
    }

    @Test
    void shouldRejectUnsupportedFileFormat() throws Exception {
        Organization organization = organization("megafaculty");
        User manager = manager(organization);

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "list.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "содержимое".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(
            asMultipart(multipart("/api/v1/organizations/{id}/imports", organization.getId()).file(file), manager)
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("IMPORT_FILE_INVALID"));
    }

    @Test
    void shouldForbidImportForRegularParticipant() throws Exception {
        Organization organization = organization("megafaculty");
        manager(organization);

        User member = account("465840");

        participant(organization, "465840", member);

        String csv = """
            Номер ИСУ,Фамилия,Имя,Email
            465830,Петров,Пётр,petrov@niuitmo.ru
            """;

        mockMvc.perform(upload(organization.getId(), csv, member))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldCancelImportBatch() throws Exception {
        Organization organization = organization("megafaculty");
        User manager = manager(organization);

        String csv = """
            Номер ИСУ,Фамилия,Имя,Email
            465830,Петров,Пётр,petrov@niuitmo.ru
            """;

        long batchId = uploadAndGetId(organization.getId(), csv, manager);

        mockMvc.perform(
            asUser(post("/api/v1/organizations/{id}/imports/{batchId}/cancel", organization.getId(), batchId), manager)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(
            asUser(post("/api/v1/organizations/{id}/imports/{batchId}/apply", organization.getId(), batchId), manager)
        )
            .andExpect(status().isConflict());
    }

    private void applyImport(Long organizationId, String csv, User actor) throws Exception {
        long batchId = uploadAndGetId(organizationId, csv, actor);

        mockMvc.perform(
            asUser(post("/api/v1/organizations/{id}/imports/{batchId}/apply", organizationId, batchId), actor)
        )
            .andExpect(status().isOk());
    }

    private long uploadAndGetId(Long organizationId, String csv, User actor) throws Exception {
        String response = mockMvc.perform(upload(organizationId, csv, actor))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        return objectMapper.readTree(response).get("batch").get("id").asLong();
    }

    private MockHttpServletRequestBuilder upload(Long organizationId, String csv, User actor) {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "list.csv",
            "text/csv",
            csv.getBytes(StandardCharsets.UTF_8)
        );

        return asMultipart(
            multipart("/api/v1/organizations/{id}/imports", organizationId).file(file),
            actor
        );
    }

    private MockHttpServletRequestBuilder asMultipart(
        MockHttpServletRequestBuilder builder,
        User actor
    ) {
        return asUser(builder, actor);
    }

    private MockHttpServletRequestBuilder asUser(
        MockHttpServletRequestBuilder builder,
        User user
    ) {
        return builder.with(jwt().jwt(token -> token.subject(String.valueOf(user.getId()))));
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

            return userRepository.save(user);
        });
    }
}
