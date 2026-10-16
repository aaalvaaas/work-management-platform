import { useCallback, useState } from "react";
import type { FormEvent } from "react";
import { Link, useParams } from "react-router-dom";
import {
    assignManager,
    excludeParticipant,
    getOrganization,
    grantAdmin,
    listParticipants,
    listRoles,
    renameOrganization,
    revokeAdmin
} from "../api/organizations";
import { createBlock, listBlocks } from "../api/blocks";
import { useAuth } from "../auth/AuthContext";
import { ErrorMessage } from "../components/ErrorMessage";
import { Loader } from "../components/Loader";
import { useResource } from "../components/useResource";

export function OrganizationPage() {
    const { organizationId } = useParams();
    const id = Number(organizationId);
    const { isPlatformAdmin } = useAuth();

    const organization = useResource(useCallback(() => getOrganization(id), [id]), [id]);
    const roles = useResource(useCallback(() => listRoles(id), [id]), [id]);
    const participants = useResource(useCallback(() => listParticipants(id), [id]), [id]);
    const blocks = useResource(useCallback(() => listBlocks(id), [id]), [id]);

    const [error, setError] = useState<unknown>(null);
    const [name, setName] = useState("");
    const [managerIsu, setManagerIsu] = useState("");
    const [adminIsu, setAdminIsu] = useState("");
    const [blockName, setBlockName] = useState("");

    const run = async (action: () => Promise<unknown>, after: () => void) => {
        setError(null);

        try {
            await action();

            after();
        } catch (failure) {
            setError(failure);
        }
    };

    const rename = (event: FormEvent) => {
        event.preventDefault();

        run(() => renameOrganization(id, name), () => {
            setName("");
            organization.reload();
        });
    };

    const setManager = (event: FormEvent) => {
        event.preventDefault();

        run(() => assignManager(id, managerIsu), () => {
            setManagerIsu("");
            organization.reload();
            roles.reload();
        });
    };

    const addAdmin = (event: FormEvent) => {
        event.preventDefault();

        run(() => grantAdmin(id, adminIsu), () => {
            setAdminIsu("");
            roles.reload();
        });
    };

    const addBlock = (event: FormEvent) => {
        event.preventDefault();

        run(() => createBlock(id, blockName), () => {
            setBlockName("");
            blocks.reload();
        });
    };

    if (organization.loading) {
        return <Loader />;
    }

    if (!organization.data) {
        return <ErrorMessage error={organization.error} />;
    }

    return (
        <>
            <h1>{organization.data.name}</h1>

            <section className="card">
                <dl className="facts">
                    <dt>Код</dt>
                    <dd>{organization.data.code}</dd>

                    <dt>Статус</dt>
                    <dd>{organization.data.status}</dd>

                    <dt>Руководитель</dt>
                    <dd>{organization.data.managerIsuNumber ?? "не назначен"}</dd>
                </dl>

                <ErrorMessage error={error} />

                <form className="row" onSubmit={rename}>
                    <div>
                        <label htmlFor="organizationName">Переименовать</label>
                        <input
                            id="organizationName"
                            value={name}
                            onChange={(event) => setName(event.target.value)}
                        />
                    </div>

                    <button type="submit" className="secondary">
                        Сохранить
                    </button>
                </form>

                {isPlatformAdmin && (
                    <form className="row" onSubmit={setManager}>
                        <div>
                            <label htmlFor="managerIsu">Назначить руководителя (номер ИСУ)</label>
                            <input
                                id="managerIsu"
                                value={managerIsu}
                                onChange={(event) => setManagerIsu(event.target.value)}
                            />
                        </div>

                        <button type="submit" className="secondary">
                            Назначить
                        </button>
                    </form>
                )}
            </section>

            <section className="card">
                <h2>Роли</h2>

                <ErrorMessage error={roles.error} />

                <table>
                    <thead>
                        <tr>
                            <th>Сотрудник</th>
                            <th>Номер ИСУ</th>
                            <th>Роль</th>
                            <th />
                        </tr>
                    </thead>
                    <tbody>
                        {roles.data?.map((role) => (
                            <tr key={`${role.userId}-${role.role}`}>
                                <td>{role.fullName}</td>
                                <td className="muted">{role.isuNumber}</td>
                                <td>
                                    <span className="tag">{role.role}</span>
                                </td>
                                <td>
                                    {role.role === "ADMIN" && (
                                        <button
                                            type="button"
                                            className="link"
                                            onClick={() =>
                                                run(
                                                    () => revokeAdmin(id, role.isuNumber),
                                                    roles.reload
                                                )
                                            }
                                        >
                                            снять
                                        </button>
                                    )}
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>

                <form className="row" onSubmit={addAdmin}>
                    <div>
                        <label htmlFor="adminIsu">Выдать права администратора (номер ИСУ)</label>
                        <input
                            id="adminIsu"
                            value={adminIsu}
                            onChange={(event) => setAdminIsu(event.target.value)}
                        />
                    </div>

                    <button type="submit" className="secondary">
                        Выдать
                    </button>
                </form>
            </section>

            <section className="card">
                <h2>Блоки</h2>

                <ErrorMessage error={blocks.error} />

                <table>
                    <thead>
                        <tr>
                            <th>Название</th>
                            <th>Статус</th>
                            <th>Руководитель</th>
                        </tr>
                    </thead>
                    <tbody>
                        {blocks.data?.map((block) => (
                            <tr key={block.id}>
                                <td>
                                    <Link to={`/organizations/${id}/blocks/${block.id}`}>
                                        {block.name}
                                    </Link>
                                </td>
                                <td>
                                    <span className="tag">{block.status}</span>
                                </td>
                                <td className="muted">{block.managerIsuNumber ?? "не назначен"}</td>
                            </tr>
                        ))}
                    </tbody>
                </table>

                <form className="row" onSubmit={addBlock}>
                    <div>
                        <label htmlFor="blockName">Новый блок</label>
                        <input
                            id="blockName"
                            value={blockName}
                            onChange={(event) => setBlockName(event.target.value)}
                        />
                    </div>

                    <button type="submit" className="secondary">
                        Создать
                    </button>
                </form>
            </section>

            <section className="card">
                <h2>Участники</h2>

                <p>
                    <Link to={`/organizations/${id}/import`}>Загрузить список из файла</Link>
                </p>

                <ErrorMessage error={participants.error} />

                <table>
                    <thead>
                        <tr>
                            <th>Участник</th>
                            <th>Номер ИСУ</th>
                            <th>Почта</th>
                            <th>Статус</th>
                            <th />
                        </tr>
                    </thead>
                    <tbody>
                        {participants.data?.map((participant) => (
                            <tr key={participant.id}>
                                <td>
                                    {participant.lastName} {participant.firstName}
                                    {participant.registered ? "" : " (не зарегистрирован)"}
                                </td>
                                <td className="muted">{participant.isuNumber}</td>
                                <td className="muted">{participant.email}</td>
                                <td>
                                    <span className="tag">{participant.status}</span>
                                </td>
                                <td>
                                    {participant.status === "ACTIVE" && (
                                        <button
                                            type="button"
                                            className="link"
                                            onClick={() =>
                                                run(
                                                    () =>
                                                        excludeParticipant(
                                                            id,
                                                            participant.isuNumber
                                                        ),
                                                    participants.reload
                                                )
                                            }
                                        >
                                            исключить
                                        </button>
                                    )}
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </section>
        </>
    );
}
