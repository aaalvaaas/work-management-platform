package ru.itmo.wmp.tasks.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.wmp.tasks.application.deck.DeckProjectionService;
import ru.itmo.wmp.tasks.application.repository.TaskRepository;
import ru.itmo.wmp.tasks.domain.Task;
import ru.itmo.wmp.tasks.domain.TaskSource;
import ru.itmo.wmp.tasks.domain.TaskStatus;
import ru.itmo.wmp.tasks.domain.TaskStatusTransition;
import ru.itmo.wmp.tasks.exception.TaskAccessDeniedException;
import ru.itmo.wmp.tasks.exception.TaskNotFoundException;
import ru.itmo.wmp.tasks.exception.TaskTransitionException;
import ru.itmo.wmp.tasks.exception.TaskValidationException;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskService {
    private final TaskRepository taskRepository;
    private final PlatformGateway platformGateway;
    private final DeckProjectionService deckProjectionService;

    public Task create(TaskCommand command, Long authorUserId) {
        OrganizationMembership membership = platformGateway.membershipOf(command.organizationId());

        requireAuthorRights(membership, command.blockId());

        if (command.blockId() != null) {
            platformGateway.blockOf(command.organizationId(), command.blockId());
        }

        Task task = new Task();

        task.setOrganizationId(command.organizationId());
        task.setBlockId(command.blockId());
        task.setTitle(command.title());
        task.setDescription(command.description());
        task.setStatus(TaskStatus.NEW);
        task.setRequiresApproval(command.requiresApproval());
        task.setSource(TaskSource.MANUAL);
        task.setAuthorUserId(authorUserId);
        task.setDueDate(command.dueDate());

        assign(task, command.assigneeIsuNumber());

        return projected(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public List<Task> findVisible(Long organizationId, TaskStatus status, Long userId) {
        OrganizationMembership membership = platformGateway.membershipOf(organizationId);

        if (!membership.hasAccess()) {
            throw new TaskAccessDeniedException("User is not a member of the organization");
        }

        List<Task> tasks = visibleTasks(organizationId, membership, userId);

        return status == null
            ? tasks
            : tasks.stream().filter(task -> task.getStatus() == status).toList();
    }

    @Transactional(readOnly = true)
    public Task getVisible(Long taskId, Long userId) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new TaskNotFoundException(taskId));

        OrganizationMembership membership = platformGateway.membershipOf(task.getOrganizationId());

        if (!canSee(task, membership, userId)) {
            throw new TaskAccessDeniedException("Task id=" + taskId + " is not visible to the user");
        }

        return task;
    }

    public Task update(Long taskId, TaskCommand command, Long userId) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new TaskNotFoundException(taskId));

        OrganizationMembership membership = platformGateway.membershipOf(task.getOrganizationId());

        requireAuthorRights(membership, task.getBlockId());

        if (command.blockId() != null && !Objects.equals(command.blockId(), task.getBlockId())) {
            platformGateway.blockOf(task.getOrganizationId(), command.blockId());

            task.setBlockId(command.blockId());
        }

        if (command.title() != null) {
            task.setTitle(command.title());
        }

        if (command.description() != null) {
            task.setDescription(command.description());
        }

        if (command.dueDate() != null) {
            task.setDueDate(command.dueDate());
        }

        if (command.assigneeIsuNumber() != null) {
            assign(task, command.assigneeIsuNumber());
        }

        task.setRequiresApproval(command.requiresApproval());

        return projected(taskRepository.save(task));
    }

    public Task requestDeckSync(Long taskId, Long userId) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new TaskNotFoundException(taskId));

        OrganizationMembership membership = platformGateway.membershipOf(task.getOrganizationId());

        requireAuthorRights(membership, task.getBlockId());

        deckProjectionService.markPending(task);

        return task;
    }

    public Task changeStatus(Long taskId, TaskStatus target, Long userId) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new TaskNotFoundException(taskId));

        OrganizationMembership membership = platformGateway.membershipOf(task.getOrganizationId());

        if (!canChangeStatus(task, membership, userId)) {
            throw new TaskAccessDeniedException("User may not change the status of this task");
        }

        if (!TaskStatusTransition.isAllowed(task.getStatus(), target)) {
            throw new TaskTransitionException(
                "Task id=" + taskId + " cannot move from " + task.getStatus() + " to " + target
            );
        }

        if (TaskStatusTransition.needsApproval(task, target)) {
            throw new TaskTransitionException(
                "Task id=" + taskId + " requires a decision of the commission before it is closed"
            );
        }

        task.setStatus(target);

        return projected(taskRepository.save(task));
    }

    private Task projected(Task task) {
        deckProjectionService.markPending(task);

        return task;
    }

    private List<Task> visibleTasks(
        Long organizationId,
        OrganizationMembership membership,
        Long userId
    ) {
        if (membership.canManageOrganization()) {
            return taskRepository.findByOrganizationIdOrderByIdDesc(organizationId);
        }

        List<Long> managedBlocks = membership.managedBlocks();

        if (managedBlocks.isEmpty()) {
            return taskRepository.findByOrganizationIdAndAssigneeUserIdOrderByIdDesc(
                organizationId,
                userId
            );
        }

        List<Task> blockTasks = taskRepository.findByOrganizationIdAndBlockIdInOrderByIdDesc(
            organizationId,
            managedBlocks
        );

        List<Task> ownTasks = taskRepository.findByOrganizationIdAndAssigneeUserIdOrderByIdDesc(
            organizationId,
            userId
        );

        return java.util.stream.Stream.concat(blockTasks.stream(), ownTasks.stream())
            .distinct()
            .sorted((first, second) -> Long.compare(second.getId(), first.getId()))
            .toList();
    }

    private boolean canSee(Task task, OrganizationMembership membership, Long userId) {
        return membership.canManageOrganization()
            || membership.managesBlock(task.getBlockId())
            || Objects.equals(task.getAssigneeUserId(), userId)
            || Objects.equals(task.getAuthorUserId(), userId);
    }

    private boolean canChangeStatus(Task task, OrganizationMembership membership, Long userId) {
        return membership.canManageOrganization()
            || membership.managesBlock(task.getBlockId())
            || Objects.equals(task.getAssigneeUserId(), userId);
    }

    private void requireAuthorRights(OrganizationMembership membership, Long blockId) {
        boolean allowed = membership.canManageOrganization() || membership.managesBlock(blockId);

        if (!allowed) {
            throw new TaskAccessDeniedException(
                "Only organization administrators and block managers may manage tasks"
            );
        }
    }

    private void assign(Task task, String isuNumber) {
        if (isuNumber == null || isuNumber.isBlank()) {
            task.setAssigneeIsuNumber(null);
            task.setAssigneeUserId(null);

            return;
        }

        ParticipantSummary participant = platformGateway.requireActiveParticipant(
            task.getOrganizationId(),
            isuNumber
        );

        if (participant.userId() == null) {
            throw new TaskValidationException(
                "Participant " + isuNumber + " has not registered an account yet"
            );
        }

        task.setAssigneeIsuNumber(participant.isuNumber());
        task.setAssigneeUserId(participant.userId());
    }
}
