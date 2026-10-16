import { api, setAccessToken } from "./client";
import type { TokenResponse, User } from "./types";

export async function login(isuNumber: string, password: string): Promise<User> {
    const tokens = await api.post<TokenResponse>("/auth/login", { isuNumber, password });

    setAccessToken(tokens.accessToken);

    return currentUser();
}

export async function logout(): Promise<void> {
    try {
        await api.post<void>("/auth/logout");
    } finally {
        setAccessToken(null);
    }
}

export function currentUser(): Promise<User> {
    return api.get<User>("/users/me");
}
