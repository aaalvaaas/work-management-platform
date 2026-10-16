import { useCallback, useEffect, useState } from "react";

interface Resource<T> {
    data: T | null;
    error: unknown;
    loading: boolean;
    reload: () => void;
}

export function useResource<T>(load: () => Promise<T>, deps: unknown[]): Resource<T> {
    const [data, setData] = useState<T | null>(null);
    const [error, setError] = useState<unknown>(null);
    const [loading, setLoading] = useState(true);
    const [attempt, setAttempt] = useState(0);

    const reload = useCallback(() => setAttempt((value) => value + 1), []);

    useEffect(() => {
        let active = true;

        setLoading(true);

        load()
            .then((result) => {
                if (active) {
                    setData(result);
                    setError(null);
                }
            })
            .catch((failure: unknown) => {
                if (active) {
                    setError(failure);
                }
            })
            .finally(() => {
                if (active) {
                    setLoading(false);
                }
            });

        return () => {
            active = false;
        };
    }, [...deps, attempt]);

    return { data, error, loading, reload };
}
