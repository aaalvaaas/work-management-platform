package ru.itmo.wmp.tasks.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TaskStatusTransitionTest {
    @Test
    void shouldAllowUsualProgress() {
        assertThat(TaskStatusTransition.isAllowed(TaskStatus.NEW, TaskStatus.IN_PROGRESS)).isTrue();
        assertThat(TaskStatusTransition.isAllowed(TaskStatus.IN_PROGRESS, TaskStatus.DONE)).isTrue();
    }

    @Test
    void shouldNotReopenFinishedTask() {
        assertThat(TaskStatusTransition.isAllowed(TaskStatus.DONE, TaskStatus.IN_PROGRESS)).isFalse();
        assertThat(TaskStatusTransition.isAllowed(TaskStatus.CANCELLED, TaskStatus.NEW)).isFalse();
    }

    @Test
    void shouldRequireApprovalBeforeClosing() {
        Task task = new Task();

        task.setRequiresApproval(true);
        task.setStatus(TaskStatus.IN_PROGRESS);

        assertThat(TaskStatusTransition.needsApproval(task, TaskStatus.DONE)).isTrue();

        task.setStatus(TaskStatus.ON_APPROVAL);

        assertThat(TaskStatusTransition.needsApproval(task, TaskStatus.DONE)).isFalse();
    }

    @Test
    void shouldNotRequireApprovalForOrdinaryTask() {
        Task task = new Task();

        task.setRequiresApproval(false);
        task.setStatus(TaskStatus.IN_PROGRESS);

        assertThat(TaskStatusTransition.needsApproval(task, TaskStatus.DONE)).isFalse();
    }
}
