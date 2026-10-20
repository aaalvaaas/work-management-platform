import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { setAccessToken } from "../api/client";
import { TasksPage } from "./TasksPage";

function json(status: number, body: unknown): Response {
    return new Response(JSON.stringify(body), {
        status,
        headers: { "Content-Type": "application/json" }
    });
}

const TASKS = [
    {
        id: 1,
        organizationId: 10,
        blockId: 3,
        title: "Подготовить справку",
        description: null,
        status: "NEW",
        requiresApproval: false,
        source: "MANUAL",
        authorUserId: 100,
        requesterEmail: null,
        assigneeUserId: 300,
        assigneeIsuNumber: "465824",
        dueDate: "2026-10-30",
        createdAt: "2026-10-17T10:00:00",
        updatedAt: "2026-10-17T10:00:00"
    },
    {
        id: 2,
        organizationId: 10,
        blockId: null,
        title: "Согласовать закупку",
        description: null,
        status: "ON_APPROVAL",
        requiresApproval: true,
        source: "MAIL",
        authorUserId: null,
        requesterEmail: "ivanov@niuitmo.ru",
        assigneeUserId: null,
        assigneeIsuNumber: null,
        dueDate: null,
        createdAt: "2026-10-17T10:00:00",
        updatedAt: "2026-10-17T10:00:00"
    }
];

const BLOCKS = [
    {
        id: 3,
        organizationId: 10,
        name: "Разработка",
        status: "ACTIVE",
        managerIsuNumber: null,
        createdAt: "2026-10-01T10:00:00"
    }
];

function stub(onPost?: () => Response) {
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
        const url = String(input);

        if (init?.method === "POST") {
            return onPost ? onPost() : json(201, TASKS[0]);
        }

        if (url.includes("/blocks")) {
            return json(200, BLOCKS);
        }

        return json(200, TASKS);
    });

    vi.stubGlobal("fetch", fetchMock);

    return fetchMock;
}

function renderPage() {
    render(
        <MemoryRouter initialEntries={["/organizations/10/tasks"]}>
            <Routes>
                <Route path="/organizations/:organizationId/tasks" element={<TasksPage />} />
            </Routes>
        </MemoryRouter>
    );
}

describe("tasks page", () => {
    beforeEach(() => {
        setAccessToken("token");
        vi.restoreAllMocks();
    });

    it("places tasks in the column of their status", async () => {
        stub();

        renderPage();

        const card = await screen.findByText("Подготовить справку");
        const approval = screen.getByText("Согласовать закупку");

        expect(card.closest(".column")?.querySelector("h2")?.textContent).toBe("Новые");
        expect(approval.closest(".column")?.querySelector("h2")?.textContent).toBe(
            "На согласовании"
        );
        expect(screen.getByText("требует согласования")).toBeInTheDocument();
    });

    it("shows the block and the assignee of a task", async () => {
        stub();

        renderPage();

        expect(await screen.findByText(/Разработка · 465824 · до 2026-10-30/)).toBeInTheDocument();
        expect(screen.getByText(/без блока · без исполнителя/)).toBeInTheDocument();
    });

    it("marks a task that came from a mail", async () => {
        stub();

        renderPage();

        expect(
            await screen.findByText(/из письма · ivanov@niuitmo.ru/)
        ).toBeInTheDocument();
    });

    it("creates a task with the entered values", async () => {
        const fetchMock = stub();

        renderPage();

        await userEvent.type(await screen.findByLabelText("Название"), "Новая задача");
        await userEvent.type(screen.getByLabelText("Исполнитель (номер ИСУ)"), "465825");
        await userEvent.click(screen.getByRole("button", { name: "Создать задачу" }));

        await waitFor(() => {
            const call = fetchMock.mock.calls.find(
                (entry) => (entry[1] as RequestInit | undefined)?.method === "POST"
            );

            expect(JSON.parse(String((call?.[1] as RequestInit | undefined)?.body))).toMatchObject({
                organizationId: 10,
                title: "Новая задача",
                assigneeIsuNumber: "465825",
                requiresApproval: false
            });
        });
    });

    it("reports that a task awaiting approval cannot be closed", async () => {
        stub(() =>
            json(409, {
                error: "TASK_TRANSITION_NOT_ALLOWED",
                message: "Задачу нельзя закрыть до решения комиссии"
            })
        );

        renderPage();

        await userEvent.click(await screen.findByRole("button", { name: "в работу" }));

        expect(await screen.findByRole("alert")).toHaveTextContent(
            "Задачу нельзя закрыть до решения комиссии"
        );
    });
});
