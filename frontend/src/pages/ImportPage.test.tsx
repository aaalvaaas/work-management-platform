import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { setAccessToken } from "../api/client";
import { ImportPage } from "./ImportPage";

function json(status: number, body: unknown): Response {
    return new Response(JSON.stringify(body), {
        status,
        headers: { "Content-Type": "application/json" }
    });
}

function batch(status: string, errors: number) {
    return {
        id: 5,
        organizationId: 10,
        filename: "list.csv",
        status,
        totalRows: 2,
        created: 1,
        updated: 0,
        deactivated: 0,
        unchanged: 0,
        errors,
        createdAt: "2026-10-16T10:00:00",
        appliedAt: null
    };
}

function renderPage() {
    render(
        <MemoryRouter initialEntries={["/organizations/10/import"]}>
            <Routes>
                <Route path="/organizations/:organizationId/import" element={<ImportPage />} />
            </Routes>
        </MemoryRouter>
    );
}

async function uploadFile() {
    const input = document.querySelector("input[type=file]") as HTMLInputElement;

    await userEvent.upload(input, new File(["isu"], "list.csv", { type: "text/csv" }));
    await userEvent.click(screen.getByRole("button", { name: "Проверить файл" }));
}

describe("import page", () => {
    beforeEach(() => {
        setAccessToken("token");
        vi.restoreAllMocks();
    });

    it("shows the calculated changes and offers to apply them", async () => {
        const fetchMock = vi.fn(async (_input: RequestInfo | URL, init?: RequestInit) => {
            if (init?.method === "POST") {
                return json(200, {
                    batch: batch("VALIDATED", 0),
                    changes: [
                        {
                            rowNumber: 2,
                            type: "CREATE",
                            isuNumber: "465824",
                            lastName: "Иванов",
                            firstName: "Иван",
                            patronymic: null,
                            email: "465824@niuitmo.ru",
                            position: null,
                            blockNames: "дизайн",
                            details: "новый участник"
                        }
                    ]
                });
            }

            return json(200, []);
        });

        vi.stubGlobal("fetch", fetchMock);

        renderPage();

        await uploadFile();

        expect(await screen.findByText("новый участник")).toBeInTheDocument();
        expect(screen.getByText("добавить")).toBeInTheDocument();
        expect(screen.getByRole("button", { name: "Применить" })).toBeInTheDocument();

        const upload = fetchMock.mock.calls.find((call) => (call[1] as RequestInit)?.body instanceof FormData);

        expect(String(upload?.[0])).toBe("/api/v1/organizations/10/imports");
    });

    it("does not offer to apply a file with errors", async () => {
        const fetchMock = vi.fn(async (_input: RequestInfo | URL, init?: RequestInit) => {
            if (init?.method === "POST") {
                return json(200, {
                    batch: batch("FAILED", 1),
                    changes: [
                        {
                            rowNumber: 2,
                            type: "ERROR",
                            isuNumber: "46582",
                            lastName: "Иванов",
                            firstName: "Иван",
                            patronymic: null,
                            email: "bad",
                            position: null,
                            blockNames: null,
                            details: "номер ИСУ должен состоять из шести цифр"
                        }
                    ]
                });
            }

            return json(200, []);
        });

        vi.stubGlobal("fetch", fetchMock);

        renderPage();

        await uploadFile();

        expect(
            await screen.findByText("номер ИСУ должен состоять из шести цифр")
        ).toBeInTheDocument();
        expect(screen.queryByRole("button", { name: "Применить" })).not.toBeInTheDocument();
    });
});
