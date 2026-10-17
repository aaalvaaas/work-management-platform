import { useCallback, useState } from "react";
import type { FormEvent } from "react";
import { Link, useParams } from "react-router-dom";
import { listBlocks } from "../api/blocks";
import { changeTaskStatus, createTask, listTasks } from "../api/tasks";
import type { Task, TaskStatus } from "../api/types";
import { ErrorMessage } from "../components/ErrorMessage";
import { Loader } from "../components/Loader";
import { useResource } from "../components/useResource";

const COLUMNS: { status: TaskStatus; title: string }[] = [
    { status: "NEW", title: "Новые" },
    { status: "IN_PROGRESS", title: "В работе" },
    { status: "ON_APPROVAL", title: "На согласовании" },
    { status: "DONE", title: "Готово" }
];

const NEXT_STATUS: Partial<Record<TaskStatus, { status: TaskStatus; label: string }[]>> = {
    NEW: [{ status: "IN_PROGRESS", label: "в работу" }],
    IN_PROGRESS: [
        { status: "ON_APPROVAL", label: "на согласование" },
        { status: "DONE", label: "готово" }
    ],
    ON_APPROVAL: [{ status: "DONE", label: "готово" }]
};

export function TasksPage() {
    const { organizationId } = useParams();
    const id = Number(organizationId);

    const tasks = useResource(useCallback(() => listTasks(id), [id]), [id]);
    const blocks = useResource(useCallback(() => listBlocks(id), [id]), [id]);

    const [title, setTitle] = useState("");
    const [description, setDescription] = useState("");
    const [blockId, setBlockId] = useState("");
    const [assignee, setAssignee] = useState("");
    const [dueDate, setDueDate] = useState("");
    const [requiresApproval, setRequiresApproval] = useState(false);
    const [error, setError] = useState<unknown>(null);

    const run = async (action: () => Promise<unknown>) => {
        setError(null);

        try {
            await action();

            tasks.reload();
        } catch (failure) {
            setError(failure);
        }
    };

    const submit = (event: FormEvent) => {
        event.preventDefault();

        run(async () => {
            await createTask({
                organizationId: id,
                blockId: blockId === "" ? null : Number(blockId),
                title,
                description,
                assigneeIsuNumber: assignee === "" ? null : assignee,
                dueDate: dueDate === "" ? null : dueDate,
                requiresApproval
            });

            setTitle("");
            setDescription("");
            setAssignee("");
            setDueDate("");
            setRequiresApproval(false);
        });
    };

    const blockName = (task: Task) =>
        blocks.data?.find((block) => block.id === task.blockId)?.name ?? null;

    return (
        <>
            <p className="muted">
                <Link to={`/organizations/${id}`}>← к организации</Link>
            </p>

            <h1>Задачи</h1>

            <ErrorMessage error={error} />
            <ErrorMessage error={tasks.error} />

            {tasks.loading && <Loader />}

            <div className="board">
                {COLUMNS.map((column) => (
                    <section className="column" key={column.status}>
                        <h2>{column.title}</h2>

                        {(tasks.data ?? [])
                            .filter((task) => task.status === column.status)
                            .map((task) => (
                                <article className="task" key={task.id}>
                                    <strong>{task.title}</strong>

                                    {task.requiresApproval && (
                                        <span className="tag">требует согласования</span>
                                    )}

                                    <p className="muted">
                                        {blockName(task) ?? "без блока"}
                                        {task.assigneeIsuNumber
                                            ? ` · ${task.assigneeIsuNumber}`
                                            : " · без исполнителя"}
                                        {task.dueDate ? ` · до ${task.dueDate}` : ""}
                                    </p>

                                    <div className="row">
                                        {(NEXT_STATUS[task.status] ?? []).map((move) => (
                                            <button
                                                key={move.status}
                                                type="button"
                                                className="link"
                                                onClick={() =>
                                                    run(() =>
                                                        changeTaskStatus(task.id, move.status)
                                                    )
                                                }
                                            >
                                                {move.label}
                                            </button>
                                        ))}
                                    </div>
                                </article>
                            ))}
                    </section>
                ))}
            </div>

            <form className="card" onSubmit={submit}>
                <h2>Новая задача</h2>

                <label htmlFor="taskTitle">Название</label>
                <input
                    id="taskTitle"
                    value={title}
                    onChange={(event) => setTitle(event.target.value)}
                />

                <label htmlFor="taskDescription">Описание</label>
                <input
                    id="taskDescription"
                    value={description}
                    onChange={(event) => setDescription(event.target.value)}
                />

                <div className="row">
                    <div>
                        <label htmlFor="taskBlock">Блок</label>
                        <select
                            id="taskBlock"
                            value={blockId}
                            onChange={(event) => setBlockId(event.target.value)}
                        >
                            <option value="">без блока</option>
                            {blocks.data?.map((block) => (
                                <option key={block.id} value={block.id}>
                                    {block.name}
                                </option>
                            ))}
                        </select>
                    </div>

                    <div>
                        <label htmlFor="taskAssignee">Исполнитель (номер ИСУ)</label>
                        <input
                            id="taskAssignee"
                            value={assignee}
                            onChange={(event) => setAssignee(event.target.value)}
                        />
                    </div>

                    <div>
                        <label htmlFor="taskDueDate">Срок</label>
                        <input
                            id="taskDueDate"
                            type="date"
                            value={dueDate}
                            onChange={(event) => setDueDate(event.target.value)}
                        />
                    </div>
                </div>

                <label htmlFor="taskApproval" className="row">
                    <input
                        id="taskApproval"
                        type="checkbox"
                        checked={requiresApproval}
                        onChange={(event) => setRequiresApproval(event.target.checked)}
                    />
                    требует согласования комиссией
                </label>

                <button type="submit">Создать задачу</button>
            </form>
        </>
    );
}
