import { useState } from "react";
import type { FormEvent } from "react";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { ErrorMessage } from "../components/ErrorMessage";
import { Loader } from "../components/Loader";

export function LoginPage() {
    const { status, login } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();

    const [isuNumber, setIsuNumber] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState<unknown>(null);
    const [submitting, setSubmitting] = useState(false);

    if (status === "loading") {
        return <Loader label="Проверяем сессию" />;
    }

    if (status === "authenticated") {
        return <Navigate to="/organizations" replace />;
    }

    const submit = async (event: FormEvent) => {
        event.preventDefault();

        setSubmitting(true);
        setError(null);

        try {
            await login(isuNumber, password);

            const from = (location.state as { from?: string } | null)?.from;

            navigate(from ?? "/organizations", { replace: true });
        } catch (failure) {
            setError(failure);
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div className="centered">
            <form className="card narrow" onSubmit={submit}>
                <h1>Вход</h1>

                <label htmlFor="isuNumber">Номер ИСУ</label>
                <input
                    id="isuNumber"
                    name="isuNumber"
                    inputMode="numeric"
                    autoComplete="username"
                    value={isuNumber}
                    onChange={(event) => setIsuNumber(event.target.value)}
                />

                <label htmlFor="password">Пароль</label>
                <input
                    id="password"
                    name="password"
                    type="password"
                    autoComplete="current-password"
                    value={password}
                    onChange={(event) => setPassword(event.target.value)}
                />

                <ErrorMessage error={error} />

                <button type="submit" disabled={submitting}>
                    {submitting ? "Входим…" : "Войти"}
                </button>

                <p className="muted">
                    Нет аккаунта? <Link to="/register">Подать заявку на регистрацию</Link>
                </p>
            </form>
        </div>
    );
}
