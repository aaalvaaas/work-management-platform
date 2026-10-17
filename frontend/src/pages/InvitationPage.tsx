import { useCallback, useState } from "react";
import type { FormEvent } from "react";
import { Link, useParams } from "react-router-dom";
import { completeRegistration, registrationDetails } from "../api/registration";
import { ErrorMessage } from "../components/ErrorMessage";
import { Loader } from "../components/Loader";
import { useResource } from "../components/useResource";

export function InvitationPage() {
    const { token } = useParams();
    const invitationToken = token ?? "";

    const details = useResource(
        useCallback(() => registrationDetails(invitationToken), [invitationToken]),
        [invitationToken]
    );

    const [password, setPassword] = useState("");
    const [error, setError] = useState<unknown>(null);
    const [submitting, setSubmitting] = useState(false);
    const [done, setDone] = useState(false);

    const submit = async (event: FormEvent) => {
        event.preventDefault();

        setSubmitting(true);
        setError(null);

        try {
            await completeRegistration(invitationToken, password);

            setDone(true);
        } catch (failure) {
            setError(failure);
        } finally {
            setSubmitting(false);
        }
    };

    if (details.loading) {
        return (
            <div className="centered">
                <Loader label="Проверяем ссылку" />
            </div>
        );
    }

    if (done) {
        return (
            <div className="centered">
                <section className="card narrow">
                    <h1>Регистрация завершена</h1>
                    <p>Теперь можно войти по номеру ИСУ и заданному паролю.</p>
                    <p>
                        <Link to="/login">Перейти ко входу</Link>
                    </p>
                </section>
            </div>
        );
    }

    if (!details.data) {
        return (
            <div className="centered">
                <section className="card narrow">
                    <h1>Ссылка не подошла</h1>
                    <ErrorMessage error={details.error} />
                    <p className="muted">
                        Ссылка одноразовая и живёт ограниченное время. Попросите руководителя
                        организации выдать приглашение заново.
                    </p>
                    <p>
                        <Link to="/login">Ко входу</Link>
                    </p>
                </section>
            </div>
        );
    }

    return (
        <div className="centered">
            <form className="card narrow" onSubmit={submit}>
                <h1>Завершение регистрации</h1>

                <dl className="facts">
                    <dt>Участник</dt>
                    <dd>
                        {details.data.lastName} {details.data.firstName}{" "}
                        {details.data.patronymic ?? ""}
                    </dd>

                    <dt>Номер ИСУ</dt>
                    <dd>{details.data.isuNumber}</dd>

                    <dt>Почта</dt>
                    <dd>{details.data.email}</dd>

                    <dt>Организации</dt>
                    <dd>{details.data.organizations.join(", ")}</dd>
                </dl>

                <label htmlFor="password">Придумайте пароль</label>
                <input
                    id="password"
                    type="password"
                    autoComplete="new-password"
                    value={password}
                    onChange={(event) => setPassword(event.target.value)}
                />

                <ErrorMessage error={error} />

                <button type="submit" disabled={submitting}>
                    {submitting ? "Сохраняем…" : "Завершить регистрацию"}
                </button>
            </form>
        </div>
    );
}
