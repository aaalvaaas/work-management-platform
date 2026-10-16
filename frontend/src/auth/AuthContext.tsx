import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import * as authApi from "../api/auth";
import { refreshAccessToken, setAccessToken } from "../api/client";
import type { User } from "../api/types";

type AuthStatus = "loading" | "authenticated" | "anonymous";

interface AuthValue {
    user: User | null;
    status: AuthStatus;
    login: (isuNumber: string, password: string) => Promise<void>;
    logout: () => Promise<void>;
    isPlatformAdmin: boolean;
}

const AuthContext = createContext<AuthValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
    const [user, setUser] = useState<User | null>(null);
    const [status, setStatus] = useState<AuthStatus>("loading");

    useEffect(() => {
        let active = true;

        restoreSession()
            .then((restored) => {
                if (!active) {
                    return;
                }

                setUser(restored);
                setStatus(restored ? "authenticated" : "anonymous");
            });

        return () => {
            active = false;
        };
    }, []);

    const login = useCallback(async (isuNumber: string, password: string) => {
        const loggedIn = await authApi.login(isuNumber, password);

        setUser(loggedIn);
        setStatus("authenticated");
    }, []);

    const logout = useCallback(async () => {
        await authApi.logout();

        setUser(null);
        setStatus("anonymous");
    }, []);

    const value = useMemo<AuthValue>(
        () => ({
            user,
            status,
            login,
            logout,
            isPlatformAdmin: user?.globalRoles.includes("PLATFORM_ADMIN") ?? false
        }),
        [user, status, login, logout]
    );

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthValue {
    const value = useContext(AuthContext);

    if (!value) {
        throw new Error("useAuth must be used inside AuthProvider");
    }

    return value;
}

async function restoreSession(): Promise<User | null> {
    const token = await refreshAccessToken();

    if (!token) {
        return null;
    }

    try {
        return await authApi.currentUser();
    } catch {
        setAccessToken(null);

        return null;
    }
}
