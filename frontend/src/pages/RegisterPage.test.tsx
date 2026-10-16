import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { setAccessToken } from "../api/client";
import { RegisterPage } from "./RegisterPage";

function json(status: number, body: unknown): Response {
    return new Response(JSON.stringify(body), {
        status,
        headers: { "Content-Type": "application/json" }
    });
}

async function fillForm() {
    await userEvent.type(screen.getByLabelText("Код организации"), "megafaculty");
    await userEvent.type(screen.getByLabelText("Номер ИСУ"), "465830");
    await userEvent.type(screen.getByLabelText("Фамилия"), "Петров");
    await userEvent.type(screen.getByLabelText("Имя"), "Пётр");
    await userEvent.type(screen.getByLabelText("Почта"), "petrov@niuitmo.ru");
    await userEvent.type(screen.getByLabelText("Пароль"), "Str0ngPassword!");
    await userEvent.click(screen.getByRole("button", { name: "Отправить заявку" }));
}

describe("register page", () => {
    beforeEach(() => {
        setAccessToken(null);
        vi.restoreAllMocks();
    });

    it("sends the request and reports that it waits for approval", async () => {
        const fetchMock = vi.fn(async (_input: RequestInfo | URL, _init?: RequestInit) =>
            json(201, {
                id: 3,
                organizationId: 10,
                organizationName: "Мегафакультет ЦИТ",
                isuNumber: "465830",
                status: "PENDING"
            })
        );

        vi.stubGlobal("fetch", fetchMock);

        render(
            <MemoryRouter>
                <RegisterPage />
            </MemoryRouter>
        );

        await fillForm();

        expect(await screen.findByText("Заявка отправлена")).toBeInTheDocument();
        expect(screen.getByText(/Мегафакультет ЦИТ/)).toBeInTheDocument();

        const call = fetchMock.mock.calls[0];

        expect(String(call?.[0])).toBe("/api/v1/registration/requests");
        expect(JSON.parse(String(call?.[1]?.body))).toMatchObject({
            organizationCode: "megafaculty",
            isuNumber: "465830",
            lastName: "Петров"
        });
    });

    it("shows that the organization code is unknown", async () => {
        vi.stubGlobal(
            "fetch",
            vi.fn(async () =>
                json(404, {
                    error: "ORGANIZATION_NOT_FOUND",
                    message: "Организация не найдена"
                })
            )
        );

        render(
            <MemoryRouter>
                <RegisterPage />
            </MemoryRouter>
        );

        await fillForm();

        expect(await screen.findByRole("alert")).toHaveTextContent("Организация не найдена");
    });
});
