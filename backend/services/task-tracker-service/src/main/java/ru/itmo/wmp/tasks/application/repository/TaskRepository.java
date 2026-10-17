package ru.itmo.wmp.tasks.application.repository;

import ru.itmo.wmp.tasks.domain.Task;
import ru.itmo.wmp.tasks.domain.TaskStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TaskRepository {
    Optional<Task> findById(Long id);

    List<Task> findByOrganizationIdOrderByIdDesc(Long organizationId);

    List<Task> findByOrganizationIdAndStatusOrderByIdDesc(Long organizationId, TaskStatus status);

    List<Task> findByOrganizationIdAndBlockIdInOrderByIdDesc(
        Long organizationId,
        Collection<Long> blockIds
    );

    List<Task> findByOrganizationIdAndAssigneeUserIdOrderByIdDesc(
        Long organizationId,
        Long assigneeUserId
    );

    Task save(Task task);
}
