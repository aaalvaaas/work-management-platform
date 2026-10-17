package ru.itmo.wmp.tasks.api;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.wmp.tasks.api.mapper.TaskMapper;
import ru.itmo.wmp.tasks.application.TaskCommand;
import ru.itmo.wmp.tasks.application.TaskService;
import ru.itmo.wmp.tasks.config.security.JwtPrincipal;
import ru.itmo.wmp.tasks.domain.TaskStatus;
import ru.itmo.wmp.tasks.dto.ChangeTaskStatusRequest;
import ru.itmo.wmp.tasks.dto.CreateTaskRequest;
import ru.itmo.wmp.tasks.dto.TaskResponse;
import ru.itmo.wmp.tasks.dto.UpdateTaskRequest;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService taskService;
    private final TaskMapper taskMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Create a task",
        description = "Available to organization administrators and to managers of the block"
    )
    public TaskResponse create(
        @Valid @RequestBody CreateTaskRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return taskMapper.toResponse(
            taskService.create(
                new TaskCommand(
                    request.organizationId(),
                    request.blockId(),
                    request.title(),
                    request.description(),
                    request.assigneeIsuNumber(),
                    request.dueDate(),
                    request.requiresApproval()
                ),
                JwtPrincipal.userId(jwt)
            )
        );
    }

    @GetMapping
    @Operation(
        summary = "List tasks of an organization",
        description = "Administrators see everything, block managers see their blocks, "
            + "everyone else sees the tasks assigned to them"
    )
    public List<TaskResponse> list(
        @RequestParam Long organizationId,
        @RequestParam(required = false) TaskStatus status,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return taskMapper.toResponses(
            taskService.findVisible(organizationId, status, JwtPrincipal.userId(jwt))
        );
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "Read one task")
    public TaskResponse getById(
        @PathVariable Long taskId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return taskMapper.toResponse(
            taskService.getVisible(taskId, JwtPrincipal.userId(jwt))
        );
    }

    @PatchMapping("/{taskId}")
    @Operation(summary = "Update a task")
    public TaskResponse update(
        @PathVariable Long taskId,
        @Valid @RequestBody UpdateTaskRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return taskMapper.toResponse(
            taskService.update(
                taskId,
                new TaskCommand(
                    null,
                    request.blockId(),
                    request.title(),
                    request.description(),
                    request.assigneeIsuNumber(),
                    request.dueDate(),
                    request.requiresApproval()
                ),
                JwtPrincipal.userId(jwt)
            )
        );
    }

    @PostMapping("/{taskId}/status")
    @Operation(
        summary = "Change the status of a task",
        description = "A task that requires approval cannot be closed before the commission decides"
    )
    public TaskResponse changeStatus(
        @PathVariable Long taskId,
        @Valid @RequestBody ChangeTaskStatusRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return taskMapper.toResponse(
            taskService.changeStatus(taskId, request.status(), JwtPrincipal.userId(jwt))
        );
    }
}
