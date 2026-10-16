import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { AuthProvider } from "../auth/AuthContext";
import { setAccessToken } from "../api/client";
import type { GlobalRole } from "../api/types";
import { OrganizationsPage } from "./OrganizationsPage";

function json(status: number, body: unknown): Response {
    return new Response(JSON.stringify(body), {
        status,
        headers: { "Content-Type": "application/json" }
    });
}

function stubBackend(globalRoles: GlobalRole[]) {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
        const url = String(input);

        if (url.endsWith("/auth/refresh")) {
            return json(200, { accessToken: "token", tokenType: "Bearer", expiresIn: 900 });
        }

        if (url.endsWith("/users/me")) {
            return json(200, {
                id: 1,
                isuNumber: "465824",
                lastName: "Иванов",
                firstName: "Иван",
                email: "465824@niuitmo.ru",
                globalRoles
            });
        }

        return json(200, [
            {
                id: 10,
                code: "megafaculty",
                name: "Мегафакультет",
                status: "ACTIVE",
                managerIsuNumber: "465824",
                createdAt: "2026-10-01T10:00:00"
            }
        ]);
    });

    vi.stubGlobal("fetch", fetchMock);

    return fetchMock;
}

function renderPage() {
    render(
        <MemoryRouter>
            <AuthProvider>
                <OrganizationsPage />
            </AuthProvider>
        </MemoryRouter>
    );
}

describe("organizations page", () => {
    beforeEach(() => {
        setAccessToken(null);
        vi.restoreAllMocks();
    });

    it("shows the organizations of the current user", async () => {
        stubBackend([]);

        renderPage();

        expect(await screen.findByRole("link", { name: "Мегафакультет" })).toHaveAttribute(
            "href",
            "/organizations/10"
        );
        expect(screen.getByText("megafaculty")).toBeInTheDocument();
    });

    it("hides the creation form from a regular user", async () => {
        stubBackend([]);

        renderPage();

        await screen.findByRole("link", { name: "Мегафакультет" });

        expect(screen.queryByRole("button", { name: "Создать" })).not.toBeInTheDocument();
    });

    it("offers creation to the platform administrator", async () => {
        stubBackend(["PLATFORM_ADMIN"]);

        renderPage();

        expect(await screen.findByRole("button", { name: "Создать" })).toBeInTheDocument();
    });
});
