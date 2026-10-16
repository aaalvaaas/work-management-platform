import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError, api, getAccessToken, setAccessToken } from "./client";

type FetchCall = [string, RequestInit];

function callAt(fetchMock: { mock: { calls: unknown[][] } }, index: number): FetchCall {
    const call = fetchMock.mock.calls[index];

    if (!call) {
        throw new Error(`fetch was not called ${index + 1} times`);
    }

    return [String(call[0]), (call[1] ?? {}) as RequestInit];
}

function response(status: number, body: unknown): Response {
    return new Response(body === null ? null : JSON.stringify(body), {
        status,
        headers: { "Content-Type": "application/json" }
    });
}

describe("api client", () => {
    beforeEach(() => {
        setAccessToken(null);
        vi.restoreAllMocks();
    });

    it("sends the access token with the request", async () => {
        const fetchMock = vi.fn().mockResolvedValue(response(200, { id: 1 }));

        vi.stubGlobal("fetch", fetchMock);
        setAccessToken("token-1");

        await api.get("/users/me");

        const [url, init] = callAt(fetchMock, 0);
        const headers = init.headers as Headers;

        expect(url).toBe("/api/v1/users/me");
        expect(headers.get("Authorization")).toBe("Bearer token-1");
        expect(init.credentials).toBe("include");
    });

    it("refreshes the token after 401 and repeats the request", async () => {
        const fetchMock = vi
            .fn()
            .mockResolvedValueOnce(response(401, { error: "UNAUTHORIZED" }))
            .mockResolvedValueOnce(response(200, { accessToken: "token-2", expiresIn: 900 }))
            .mockResolvedValueOnce(response(200, { id: 7 }));

        vi.stubGlobal("fetch", fetchMock);
        setAccessToken("expired");

        await expect(api.get<{ id: number }>("/users/me")).resolves.toEqual({ id: 7 });

        expect(fetchMock).toHaveBeenCalledTimes(3);
        expect(callAt(fetchMock, 1)[0]).toBe("/api/v1/auth/refresh");
        expect(getAccessToken()).toBe("token-2");

        const [, retry] = callAt(fetchMock, 2);

        expect((retry.headers as Headers).get("Authorization")).toBe("Bearer token-2");
    });

    it("gives up when the refresh fails", async () => {
        const fetchMock = vi
            .fn()
            .mockResolvedValueOnce(response(401, { error: "UNAUTHORIZED" }))
            .mockResolvedValueOnce(response(401, { error: "INVALID_TOKEN" }));

        vi.stubGlobal("fetch", fetchMock);
        setAccessToken("expired");

        await expect(api.get("/users/me")).rejects.toBeInstanceOf(ApiError);

        expect(fetchMock).toHaveBeenCalledTimes(2);
        expect(getAccessToken()).toBeNull();
    });

    it("reports field errors from the backend", async () => {
        const fetchMock = vi.fn().mockResolvedValue(
            response(400, {
                error: "VALIDATION_ERROR",
                message: "Validation failed",
                fieldErrors: { isuNumber: "ISU number must contain 6 digits" }
            })
        );

        vi.stubGlobal("fetch", fetchMock);

        const failure = await api
            .post("/auth/login", { isuNumber: "abc", password: "" })
            .catch((error: unknown) => error);

        expect(failure).toBeInstanceOf(ApiError);
        expect((failure as ApiError).code).toBe("VALIDATION_ERROR");
        expect((failure as ApiError).fieldErrors["isuNumber"] ?? "").toContain("6 digits");
    });

    it("sends a file as multipart without a json content type", async () => {
        const fetchMock = vi.fn().mockResolvedValue(response(200, { batch: {}, changes: [] }));

        vi.stubGlobal("fetch", fetchMock);

        await api.upload("/organizations/1/imports", new File(["isu"], "list.csv"));

        const [, init] = callAt(fetchMock, 0);

        expect(init.body).toBeInstanceOf(FormData);
        expect((init.headers as Headers).get("Content-Type")).toBeNull();
    });
});
