import { useState } from "react";
import type { FormEvent } from "react";
import { Link } from "react-router-dom";
import { createOrganization, listOrganizations } from "../api/organizations";
import { useAuth } from "../auth/AuthContext";
import { ErrorMessage } from "../components/ErrorMessage";
import { Loader } from "../components/Loader";
import { useResource } from "../components/useResource";

export function OrganizationsPage() {
    const { isPlatformAdmin } = useAuth();
    const organizations = useResource(listOrganizations, []);

    const [code, setCode] = useState("");
    const [name, setName] = useState("");
    const [error, setError] = useState<unknown>(null);

    const create = async (event: FormEvent) => {
        event.preventDefault();
        setError(null);

        try {
            await createOrganization(code, name);

            setCode("");
            setName("");
            organizations.reload();
        } catch (failure) {
            setError(failure);
        }
    };

    return (
        <>
            <h1>Организации</h1>

            <section className="card">
                {organizations.loading && <Loader />}
                <ErrorMessage error={organizations.error} />

                {organizations.data?.length === 0 && (
                    <p className="muted">Вы пока не состоите ни в одной организации.</p>
                )}

                {organizations.data && organizations.data.length > 0 && (
                    <table>
                        <thead>
                            <tr>
                                <th>Название</th>
                                <th>Код</th>
                                <th>Статус</th>
                                <th>Руководитель</th>
                            </tr>
                        </thead>
                        <tbody>
                            {organizations.data.map((organization) => (
                                <tr key={organization.id}>
                                    <td>
                                        <Link to={`/organizations/${organization.id}`}>
                                            {organization.name}
                                        </Link>
                                    </td>
                                    <td className="muted">{organization.code}</td>
                                    <td>
                                        <span className="tag">{organization.status}</span>
                                    </td>
                                    <td className="muted">
                                        {organization.managerIsuNumber ?? "не назначен"}
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                )}
            </section>

            {isPlatformAdmin && (
                <form className="card" onSubmit={create}>
                    <h2>Новая организация</h2>

                    <div className="row">
                        <div>
                            <label htmlFor="code">Код</label>
                            <input
                                id="code"
                                value={code}
                                onChange={(event) => setCode(event.target.value)}
                            />
                        </div>

                        <div>
                            <label htmlFor="name">Название</label>
                            <input
                                id="name"
                                value={name}
                                onChange={(event) => setName(event.target.value)}
                            />
                        </div>

                        <button type="submit">Создать</button>
                    </div>

                    <ErrorMessage error={error} />
                </form>
            )}
        </>
    );
}
