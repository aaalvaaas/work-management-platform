import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { setAccessToken } from "../api/client";
import { RegistrationRequests } from "./RegistrationRequests";

function json(status: number, body: unknown): Response {
    return new Response(JSON.stringify(body), {
        status,
        headers: { "Content-Type": "application/json" }
    });
}

const PENDING = [
    {
        id: 7,
        organizationId: 10,
        organizationName: "Мегафакультет ЦИТ",
        isuNumber: "465830",
        lastName: "Петров",
        firstName: "Пётр",
        patronymic: null,
        email: "petrov@niuitmo.ru",
        position: "студент",
        status: "PENDING",
        decisionComment: null,
        createdAt: "2026-10-16T10:00:00",
        decidedAt: null
    }
];

describe("registration requests", () => {
    beforeEach(() => {
        setAccessToken("token");
        vi.restoreAllMocks();
    });

    it("shows pending requests and approves one", async () => {
        const fetchMock = vi.fn(async (_input: RequestInfo | URL, init?: RequestInit) =>
            init?.method === "POST" ? json(200, { ...PENDING[0], status: "APPROVED" }) : json(200, PENDING)
        );

        vi.stubGlobal("fetch", fetchMock);

        render(<RegistrationRequests organizationId={10} />);

        expect(await screen.findByText("465830")).toBeInTheDocument();

        await userEvent.click(screen.getByRole("button", { name: "одобрить" }));

        await waitFor(() => {
            const approve = fetchMock.mock.calls.find((call) =>
                String(call[0]).endsWith("/registration-requests/7/approve")
            );

            expect(approve).toBeDefined();
        });
    });

    it("asks for a reason before rejecting", async () => {
        const fetchMock = vi.fn(async (_input: RequestInfo | URL, init?: RequestInit) =>
            init?.method === "POST" ? json(200, { ...PENDING[0], status: "REJECTED" }) : json(200, PENDING)
        );

        vi.stubGlobal("fetch", fetchMock);

        render(<RegistrationRequests organizationId={10} />);

        await userEvent.click(await screen.findByRole("button", { name: "отклонить" }));
        await userEvent.type(screen.getByLabelText("Причина отказа"), "Нет такого человека");
        await userEvent.click(screen.getByRole("button", { name: "подтвердить" }));

        await waitFor(() => {
            const reject = fetchMock.mock.calls.find((call) =>
                String(call[0]).endsWith("/registration-requests/7/reject")
            );

            expect(JSON.parse(String((reject?.[1] as RequestInit | undefined)?.body))).toEqual({
                comment: "Нет такого человека"
            });
        });
    });

    it("shows the invitation link for the manager to pass on", async () => {
        const fetchMock = vi.fn(async (_input: RequestInfo | URL, init?: RequestInit) =>
            init?.method === "POST"
                ? json(201, {
                      id: 1,
                      isuNumber: "465824",
                      email: "i***@niuitmo.ru",
                      organizationId: 10,
                      source: "MANAGER_INVITE",
                      expiresAt: "2026-10-19T10:00:00",
                      registrationToken: "abc123"
                  })
                : json(200, [])
        );

        vi.stubGlobal("fetch", fetchMock);

        render(<RegistrationRequests organizationId={10} />);

        await userEvent.type(
            await screen.findByLabelText("Пригласить участника (номер ИСУ)"),
            "465824"
        );
        await userEvent.click(screen.getByRole("button", { name: "Выдать ссылку" }));

        const link = await screen.findByLabelText(
            "Ссылка для регистрации, передайте её человеку"
        );

        expect((link as HTMLInputElement).value).toContain("/registration/abc123");
    });
});
