import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { setAccessToken } from "../api/client";
import { OrganizationMailbox } from "./OrganizationMailbox";

function json(status: number, body: unknown): Response {
    return new Response(JSON.stringify(body), {
        status,
        headers: { "Content-Type": "application/json" }
    });
}

const MAILBOX = {
    id: 1,
    organizationId: 10,
    host: "imap.mail.ru",
    port: 993,
    username: "megafaculty@mail.ru",
    folder: "INBOX",
    useSsl: true,
    status: "ACTIVE",
    lastError: null,
    lastCheckedAt: null
};

describe("organization mailbox", () => {
    beforeEach(() => {
        setAccessToken("token");
        vi.restoreAllMocks();
    });

    it("offers to connect a mailbox when there is none", async () => {
        vi.stubGlobal(
            "fetch",
            vi.fn(async () => json(404, { error: "MAILBOX_NOT_FOUND", message: "нет ящика" }))
        );

        render(<OrganizationMailbox organizationId={10} />);

        expect(await screen.findByRole("button", { name: "Подключить ящик" })).toBeInTheDocument();
        expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    });

    it("sends the settings and the password", async () => {
        const fetchMock = vi.fn(async (_input: RequestInfo | URL, init?: RequestInit) =>
            init?.method === "PUT"
                ? json(200, MAILBOX)
                : json(404, { error: "MAILBOX_NOT_FOUND", message: "нет ящика" })
        );

        vi.stubGlobal("fetch", fetchMock);

        render(<OrganizationMailbox organizationId={10} />);

        await userEvent.type(await screen.findByLabelText("Сервер IMAP"), "imap.mail.ru");
        await userEvent.type(screen.getByLabelText("Адрес или логин"), "megafaculty@mail.ru");
        await userEvent.type(screen.getByLabelText(/Пароль/), "secret");
        await userEvent.click(screen.getByRole("button", { name: "Подключить ящик" }));

        await waitFor(() => {
            const call = fetchMock.mock.calls.find(
                (entry) => (entry[1] as RequestInit | undefined)?.method === "PUT"
            );

            expect(JSON.parse(String((call?.[1] as RequestInit | undefined)?.body))).toMatchObject({
                host: "imap.mail.ru",
                username: "megafaculty@mail.ru",
                password: "secret",
                port: 993
            });
        });
    });

    it("shows the state of a connected mailbox and keeps the password empty", async () => {
        vi.stubGlobal("fetch", vi.fn(async () => json(200, MAILBOX)));

        render(<OrganizationMailbox organizationId={10} />);

        expect(await screen.findByText("ACTIVE")).toBeInTheDocument();

        const password = screen.getByLabelText(/Пароль/) as HTMLInputElement;

        expect(password.value).toBe("");
        expect(screen.getByRole("button", { name: "Отключить" })).toBeInTheDocument();
    });
});
