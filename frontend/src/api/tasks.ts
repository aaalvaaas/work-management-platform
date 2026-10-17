import { api } from "./client";
import type { NewTask, Task, TaskStatus } from "./types";

export const listTasks = (organizationId: number) =>
    api.get<Task[]>(`/tasks?organizationId=${organizationId}`);

export const createTask = (task: NewTask) => api.post<Task>("/tasks", task);

export const changeTaskStatus = (taskId: number, status: TaskStatus) =>
    api.post<Task>(`/tasks/${taskId}/status`, { status });

export const updateTask = (taskId: number, changes: Partial<NewTask>) =>
    api.patch<Task>(`/tasks/${taskId}`, changes);
