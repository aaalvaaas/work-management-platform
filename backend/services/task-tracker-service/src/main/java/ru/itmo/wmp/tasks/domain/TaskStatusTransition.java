package ru.itmo.wmp.tasks.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class TaskStatusTransition {
    private static final Map<TaskStatus, Set<TaskStatus>> ALLOWED = Map.of(
        TaskStatus.NEW, EnumSet.of(TaskStatus.IN_PROGRESS, TaskStatus.ON_APPROVAL, TaskStatus.DONE, TaskStatus.CANCELLED),
        TaskStatus.IN_PROGRESS, EnumSet.of(TaskStatus.NEW, TaskStatus.ON_APPROVAL, TaskStatus.DONE, TaskStatus.CANCELLED),
        TaskStatus.ON_APPROVAL, EnumSet.of(TaskStatus.IN_PROGRESS, TaskStatus.DONE, TaskStatus.CANCELLED),
        TaskStatus.DONE, EnumSet.noneOf(TaskStatus.class),
        TaskStatus.CANCELLED, EnumSet.noneOf(TaskStatus.class)
    );

    private TaskStatusTransition() {
    }

    public static boolean isAllowed(TaskStatus from, TaskStatus to) {
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    public static boolean needsApproval(Task task, TaskStatus to) {
        return task.isRequiresApproval()
            && to == TaskStatus.DONE
            && task.getStatus() != TaskStatus.ON_APPROVAL;
    }
}
