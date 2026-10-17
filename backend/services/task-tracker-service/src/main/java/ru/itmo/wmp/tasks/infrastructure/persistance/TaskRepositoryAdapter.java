package ru.itmo.wmp.tasks.infrastructure.persistance;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import ru.itmo.wmp.tasks.application.repository.TaskRepository;
import ru.itmo.wmp.tasks.domain.Task;
import ru.itmo.wmp.tasks.domain.TaskStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TaskRepositoryAdapter implements TaskRepository {
    private final JpaTaskRepository taskRepository;

    @Override
    public Optional<Task> findById(Long id) {
        return taskRepository.findById(id);
    }

    @Override
    public List<Task> findByOrganizationIdOrderByIdDesc(Long organizationId) {
        return taskRepository.findByOrganizationIdOrderByIdDesc(organizationId);
    }

    @Override
    public List<Task> findByOrganizationIdAndStatusOrderByIdDesc(
        Long organizationId,
        TaskStatus status
    ) {
        return taskRepository.findByOrganizationIdAndStatusOrderByIdDesc(organizationId, status);
    }

    @Override
    public List<Task> findByOrganizationIdAndBlockIdInOrderByIdDesc(
        Long organizationId,
        Collection<Long> blockIds
    ) {
        return taskRepository.findByOrganizationIdAndBlockIdInOrderByIdDesc(organizationId, blockIds);
    }

    @Override
    public List<Task> findByOrganizationIdAndAssigneeUserIdOrderByIdDesc(
        Long organizationId,
        Long assigneeUserId
    ) {
        return taskRepository.findByOrganizationIdAndAssigneeUserIdOrderByIdDesc(
            organizationId,
            assigneeUserId
        );
    }

    @Override
    public Task save(Task task) {
        return taskRepository.save(task);
    }
}
