package ru.itmo.wmp.tasks.infrastructure.persistance;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.wmp.tasks.domain.Task;
import ru.itmo.wmp.tasks.domain.TaskStatus;

import java.util.Collection;
import java.util.List;

public interface JpaTaskRepository extends JpaRepository<Task, Long> {
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
}
