package ru.itmo.wmp.tasks.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import ru.itmo.wmp.tasks.IntegrationTest;
import ru.itmo.wmp.tasks.application.OrganizationMembership;
import ru.itmo.wmp.tasks.application.ParticipantSummary;
import ru.itmo.wmp.tasks.application.PlatformGateway;
import ru.itmo.wmp.tasks.domain.TaskStatus;
import ru.itmo.wmp.tasks.dto.ChangeTaskStatusRequest;
import ru.itmo.wmp.tasks.dto.CreateTaskRequest;
import ru.itmo.wmp.tasks.exception.TaskValidationException;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
public class TaskControllerIntegrationTest {
    private static final Long ORGANIZATION = 10L;
    private static final Long BLOCK = 3L;
    private static final Long OTHER_BLOCK = 4L;
    private static final Long MANAGER_ID = 100L;
    private static final Long BLOCK_MANAGER_ID = 200L;
    private static final Long MEMBER_ID = 300L;
    private static final String ASSIGNEE_ISU = "465824";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PlatformGateway platformGateway;

    @BeforeEach
    void stubPlatform() {
        when(platformGateway.requireActiveParticipant(anyLong(), anyString()))
            .thenReturn(new ParticipantSummary(ASSIGNEE_ISU, MEMBER_ID, "Иванов Иван"));
    }

    @Test
    void shouldCreateTaskByOrganizationManager() throws Exception {
        asManager();

        mockMvc.perform(createRequest(BLOCK, ASSIGNEE_ISU, false, MANAGER_ID))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("NEW"))
            .andExpect(jsonPath("$.title").value("Подготовить справку"))
            .andExpect(jsonPath("$.assigneeIsuNumber").value(ASSIGNEE_ISU))
            .andExpect(jsonPath("$.assigneeUserId").value(MEMBER_ID))
            .andExpect(jsonPath("$.source").value("MANUAL"));
    }

    @Test
    void shouldLetBlockManagerCreateTaskInOwnBlockOnly() throws Exception {
        asBlockManager();

        mockMvc.perform(createRequest(BLOCK, ASSIGNEE_ISU, false, BLOCK_MANAGER_ID))
            .andExpect(status().isCreated());

        mockMvc.perform(createRequest(OTHER_BLOCK, ASSIGNEE_ISU, false, BLOCK_MANAGER_ID))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("ACCESS_DENIED"));
    }

    @Test
    void shouldForbidTaskCreationForRegularMember() throws Exception {
        asMember();

        mockMvc.perform(createRequest(BLOCK, ASSIGNEE_ISU, false, MEMBER_ID))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/tasks").param("organizationId", ORGANIZATION.toString()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldShowOnlyOwnTasksToRegularMember() throws Exception {
        asManager();

        createTask(BLOCK, ASSIGNEE_ISU, false, MANAGER_ID);
        createTask(BLOCK, null, false, MANAGER_ID);

        mockMvc.perform(listRequest(MANAGER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));

        asMember();

        mockMvc.perform(listRequest(MEMBER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].assigneeUserId").value(MEMBER_ID));
    }

    @Test
    void shouldLetAssigneeMoveTaskThroughStatuses() throws Exception {
        asManager();

        Long taskId = createTask(BLOCK, ASSIGNEE_ISU, false, MANAGER_ID);

        asMember();

        mockMvc.perform(statusRequest(taskId, TaskStatus.IN_PROGRESS, MEMBER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(statusRequest(taskId, TaskStatus.DONE, MEMBER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    void shouldNotCloseTaskThatRequiresApproval() throws Exception {
        asManager();

        Long taskId = createTask(BLOCK, ASSIGNEE_ISU, true, MANAGER_ID);

        mockMvc.perform(statusRequest(taskId, TaskStatus.DONE, MANAGER_ID))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("TASK_TRANSITION_NOT_ALLOWED"));

        mockMvc.perform(statusRequest(taskId, TaskStatus.ON_APPROVAL, MANAGER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ON_APPROVAL"));

        mockMvc.perform(statusRequest(taskId, TaskStatus.DONE, MANAGER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    void shouldNotReopenFinishedTask() throws Exception {
        asManager();

        Long taskId = createTask(BLOCK, ASSIGNEE_ISU, false, MANAGER_ID);

        mockMvc.perform(statusRequest(taskId, TaskStatus.DONE, MANAGER_ID))
            .andExpect(status().isOk());

        mockMvc.perform(statusRequest(taskId, TaskStatus.IN_PROGRESS, MANAGER_ID))
            .andExpect(status().isConflict());
    }

    @Test
    void shouldRefuseAssigneeWhoIsNotAnActiveParticipant() throws Exception {
        asManager();

        when(platformGateway.requireActiveParticipant(anyLong(), eq("465999")))
            .thenThrow(new TaskValidationException("Participant 465999 is not active"));

        mockMvc.perform(createRequest(BLOCK, "465999", false, MANAGER_ID))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("TASK_INVALID"));
    }

    @Test
    void shouldUpdateTaskFields() throws Exception {
        asManager();

        Long taskId = createTask(BLOCK, ASSIGNEE_ISU, false, MANAGER_ID);

        mockMvc.perform(
            patch("/api/v1/tasks/{id}", taskId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Новое название\",\"requiresApproval\":true}")
                .with(asUser(MANAGER_ID))
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Новое название"))
            .andExpect(jsonPath("$.requiresApproval").value(true));
    }

    @Test
    void shouldAnswerNotFoundForUnknownTask() throws Exception {
        asManager();

        mockMvc.perform(get("/api/v1/tasks/{id}", 999999).with(asUser(MANAGER_ID)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("TASK_NOT_FOUND"));
    }

    private Long createTask(
        Long blockId,
        String assigneeIsu,
        boolean requiresApproval,
        Long actorId
    ) throws Exception {
        String response = mockMvc.perform(createRequest(blockId, assigneeIsu, requiresApproval, actorId))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    private MockHttpServletRequestBuilder createRequest(
        Long blockId,
        String assigneeIsu,
        boolean requiresApproval,
        Long actorId
    ) throws Exception {
        CreateTaskRequest request = new CreateTaskRequest(
            ORGANIZATION,
            blockId,
            "Подготовить справку",
            "описание",
            assigneeIsu,
            null,
            requiresApproval
        );

        return post("/api/v1/tasks")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .with(asUser(actorId));
    }

    private MockHttpServletRequestBuilder listRequest(Long actorId) {
        return get("/api/v1/tasks")
            .param("organizationId", ORGANIZATION.toString())
            .with(asUser(actorId));
    }

    private MockHttpServletRequestBuilder statusRequest(
        Long taskId,
        TaskStatus status,
        Long actorId
    ) throws Exception {
        return post("/api/v1/tasks/{id}/status", taskId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new ChangeTaskStatusRequest(status)))
            .with(asUser(actorId));
    }

    private RequestPostProcessor asUser(Long userId) {
        return jwt().jwt(token -> token.subject(String.valueOf(userId)));
    }

    private void asManager() {
        when(platformGateway.membershipOf(any())).thenReturn(new OrganizationMembership(
            ORGANIZATION,
            MANAGER_ID,
            false,
            true,
            Set.of("MANAGER"),
            List.of()
        ));
    }

    private void asBlockManager() {
        when(platformGateway.membershipOf(any())).thenReturn(new OrganizationMembership(
            ORGANIZATION,
            BLOCK_MANAGER_ID,
            false,
            true,
            Set.of(),
            List.of(new OrganizationMembership.BlockMembership(BLOCK, Set.of("MANAGER")))
        ));
    }

    private void asMember() {
        when(platformGateway.membershipOf(any())).thenReturn(new OrganizationMembership(
            ORGANIZATION,
            MEMBER_ID,
            false,
            true,
            Set.of(),
            List.of()
        ));
    }
}
