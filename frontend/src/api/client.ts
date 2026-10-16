import type { TokenResponse } from "./types";

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "/api/v1";

let accessToken: string | null = null;
let refreshRequest: Promise<string | null> | null = null;

export class ApiError extends Error {
    constructor(
        readonly status: number,
        readonly code: string,
        message: string,
        readonly fieldErrors: Record<string, string> = {}
    ) {
        super(message);
    }
}

export function setAccessToken(token: string | null): void {
    accessToken = token;
}

export function getAccessToken(): string | null {
    return accessToken;
}

export function refreshAccessToken(): Promise<string | null> {
    if (!refreshRequest) {
        refreshRequest = fetch(`${BASE_URL}/auth/refresh`, {
            method: "POST",
            credentials: "include"
        })
            .then(async (response) => {
                if (!response.ok) {
                    return null;
                }

                const tokens = (await response.json()) as TokenResponse;

                return tokens.accessToken;
            })
            .catch(() => null)
            .then((token) => {
                accessToken = token;
                refreshRequest = null;

                return token;
            });
    }

    return refreshRequest;
}

export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
    let response = await send(path, init);

    if (response.status === 401 && (await refreshAccessToken()) !== null) {
        response = await send(path, init);
    }

    if (!response.ok) {
        throw await toApiError(response);
    }

    if (response.status === 204) {
        return undefined as T;
    }

    return (await response.json()) as T;
}

export const api = {
    get: <T>(path: string) => request<T>(path),
    post: <T>(path: string, body?: unknown) => request<T>(path, json("POST", body)),
    put: <T>(path: string, body?: unknown) => request<T>(path, json("PUT", body)),
    patch: <T>(path: string, body?: unknown) => request<T>(path, json("PATCH", body)),
    remove: <T>(path: string) => request<T>(path, { method: "DELETE" }),
    upload: <T>(path: string, file: File) => {
        const form = new FormData();

        form.append("file", file);

        return request<T>(path, { method: "POST", body: form });
    }
};

function json(method: string, body?: unknown): RequestInit {
    if (body === undefined) {
        return { method };
    }

    return {
        method,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body)
    };
}

async function send(path: string, init: RequestInit): Promise<Response> {
    const headers = new Headers(init.headers);

    if (accessToken) {
        headers.set("Authorization", `Bearer ${accessToken}`);
    }

    return fetch(`${BASE_URL}${path}`, { ...init, headers, credentials: "include" });
}

async function toApiError(response: Response): Promise<ApiError> {
    try {
        const body = await response.json();

        return new ApiError(
            response.status,
            body.error ?? "UNKNOWN",
            body.message ?? response.statusText,
            body.fieldErrors ?? {}
        );
    } catch {
        return new ApiError(response.status, "UNKNOWN", response.statusText);
    }
}
