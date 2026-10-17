package ru.itmo.wmp.tasks.api.mapper;

import org.springframework.stereotype.Component;
import ru.itmo.wmp.tasks.domain.Task;
import ru.itmo.wmp.tasks.dto.TaskResponse;

import java.util.List;

@Component
public class TaskMapper {
    public TaskResponse toResponse(Task task) {
        return new TaskResponse(
            task.getId(),
            task.getOrganizationId(),
            task.getBlockId(),
            task.getTitle(),
            task.getDescription(),
            task.getStatus(),
            task.isRequiresApproval(),
            task.getSource(),
            task.getAuthorUserId(),
            task.getAssigneeUserId(),
            task.getAssigneeIsuNumber(),
            task.getDueDate(),
            task.getCreatedAt(),
            task.getUpdatedAt()
        );
    }

    public List<TaskResponse> toResponses(List<Task> tasks) {
        return tasks.stream()
            .map(this::toResponse)
            .toList();
    }
}
