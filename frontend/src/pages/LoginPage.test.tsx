import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { AuthProvider } from "../auth/AuthContext";
import { setAccessToken } from "../api/client";
import { LoginPage } from "./LoginPage";

function json(status: number, body: unknown): Response {
    return new Response(JSON.stringify(body), {
        status,
        headers: { "Content-Type": "application/json" }
    });
}

function renderLogin() {
    return render(
        <MemoryRouter>
            <AuthProvider>
                <LoginPage />
            </AuthProvider>
        </MemoryRouter>
    );
}

describe("login page", () => {
    beforeEach(() => {
        setAccessToken(null);
        vi.restoreAllMocks();
    });

    it("logs in with the entered credentials", async () => {
        const fetchMock = vi.fn(async (input: RequestInfo | URL, _init?: RequestInit) => {
            const url = String(input);

            if (url.endsWith("/auth/refresh")) {
                return json(401, { error: "INVALID_TOKEN" });
            }

            if (url.endsWith("/auth/login")) {
                return json(200, { accessToken: "token", tokenType: "Bearer", expiresIn: 900 });
            }

            return json(200, {
                id: 1,
                isuNumber: "465824",
                lastName: "Иванов",
                firstName: "Иван",
                email: "465824@niuitmo.ru",
                globalRoles: []
            });
        });

        vi.stubGlobal("fetch", fetchMock);

        renderLogin();

        await screen.findByLabelText("Номер ИСУ");

        await userEvent.type(screen.getByLabelText("Номер ИСУ"), "465824");
        await userEvent.type(screen.getByLabelText("Пароль"), "Str0ngPassword!");
        await userEvent.click(screen.getByRole("button", { name: "Войти" }));

        await waitFor(() => {
            const login = fetchMock.mock.calls.find((call) => String(call[0]).endsWith("/auth/login"));

            expect(login).toBeDefined();
            expect(JSON.parse(String(login?.[1]?.body))).toEqual({
                isuNumber: "465824",
                password: "Str0ngPassword!"
            });
        });
    });

    it("shows the error returned by the backend", async () => {
        const fetchMock = vi.fn(async (input: RequestInfo | URL, _init?: RequestInit) => {
            const url = String(input);

            if (url.endsWith("/auth/refresh")) {
                return json(401, { error: "INVALID_TOKEN" });
            }

            return json(401, { error: "INVALID_CREDENTIALS", message: "Неверный номер ИСУ или пароль" });
        });

        vi.stubGlobal("fetch", fetchMock);

        renderLogin();

        await userEvent.type(await screen.findByLabelText("Номер ИСУ"), "465824");
        await userEvent.type(screen.getByLabelText("Пароль"), "wrong");
        await userEvent.click(screen.getByRole("button", { name: "Войти" }));

        expect(await screen.findByRole("alert")).toHaveTextContent("Неверный номер ИСУ или пароль");
    });
});
