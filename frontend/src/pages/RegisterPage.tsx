import { useState } from "react";
import type { ChangeEvent, FormEvent } from "react";
import { Link } from "react-router-dom";
import { submitRegistrationRequest } from "../api/registration";
import type { RegistrationSubmission } from "../api/types";
import { ErrorMessage } from "../components/ErrorMessage";

const EMPTY: RegistrationSubmission = {
    organizationCode: "",
    isuNumber: "",
    lastName: "",
    firstName: "",
    patronymic: "",
    email: "",
    position: "",
    password: ""
};

export function RegisterPage() {
    const [form, setForm] = useState<RegistrationSubmission>(EMPTY);
    const [error, setError] = useState<unknown>(null);
    const [submitting, setSubmitting] = useState(false);
    const [submitted, setSubmitted] = useState<string | null>(null);

    const change = (field: keyof RegistrationSubmission) => (
        event: ChangeEvent<HTMLInputElement>
    ) => setForm({ ...form, [field]: event.target.value });

    const submit = async (event: FormEvent) => {
        event.preventDefault();

        setSubmitting(true);
        setError(null);

        try {
            const request = await submitRegistrationRequest(form);

            setSubmitted(request.organizationName);
            setForm(EMPTY);
        } catch (failure) {
            setError(failure);
        } finally {
            setSubmitting(false);
        }
    };

    if (submitted) {
        return (
            <div className="centered">
                <section className="card narrow">
                    <h1>Заявка отправлена</h1>

                    <p>
                        Заявка на вступление в организацию «{submitted}» отправлена. Войти можно
                        будет после того, как её одобрит руководитель организации.
                    </p>

                    <p>
                        <Link to="/login">Вернуться ко входу</Link>
                    </p>
                </section>
            </div>
        );
    }

    return (
        <div className="centered">
            <form className="card narrow" onSubmit={submit}>
                <h1>Заявка на регистрацию</h1>

                <label htmlFor="organizationCode">Код организации</label>
                <input
                    id="organizationCode"
                    value={form.organizationCode}
                    onChange={change("organizationCode")}
                />

                <label htmlFor="isuNumber">Номер ИСУ</label>
                <input
                    id="isuNumber"
                    inputMode="numeric"
                    value={form.isuNumber}
                    onChange={change("isuNumber")}
                />

                <label htmlFor="lastName">Фамилия</label>
                <input id="lastName" value={form.lastName} onChange={change("lastName")} />

                <label htmlFor="firstName">Имя</label>
                <input id="firstName" value={form.firstName} onChange={change("firstName")} />

                <label htmlFor="patronymic">Отчество</label>
                <input id="patronymic" value={form.patronymic} onChange={change("patronymic")} />

                <label htmlFor="email">Почта</label>
                <input id="email" type="email" value={form.email} onChange={change("email")} />

                <label htmlFor="position">Должность</label>
                <input id="position" value={form.position} onChange={change("position")} />

                <label htmlFor="password">Пароль</label>
                <input
                    id="password"
                    type="password"
                    autoComplete="new-password"
                    value={form.password}
                    onChange={change("password")}
                />

                <ErrorMessage error={error} />

                <button type="submit" disabled={submitting}>
                    {submitting ? "Отправляем…" : "Отправить заявку"}
                </button>

                <p className="muted">
                    <Link to="/login">У меня уже есть аккаунт</Link>
                </p>
            </form>
        </div>
    );
}
