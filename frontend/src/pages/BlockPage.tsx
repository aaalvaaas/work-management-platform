import { useCallback, useState } from "react";
import type { FormEvent } from "react";
import { Link, useParams } from "react-router-dom";
import {
    addBlockMember,
    assignBlockManager,
    getBlock,
    listBlockMembers,
    removeBlockMember
} from "../api/blocks";
import { ErrorMessage } from "../components/ErrorMessage";
import { Loader } from "../components/Loader";
import { useResource } from "../components/useResource";

export function BlockPage() {
    const { organizationId, blockId } = useParams();
    const organization = Number(organizationId);
    const id = Number(blockId);

    const block = useResource(
        useCallback(() => getBlock(organization, id), [organization, id]),
        [organization, id]
    );

    const members = useResource(
        useCallback(() => listBlockMembers(organization, id), [organization, id]),
        [organization, id]
    );

    const [memberIsu, setMemberIsu] = useState("");
    const [managerIsu, setManagerIsu] = useState("");
    const [error, setError] = useState<unknown>(null);

    const run = async (action: () => Promise<unknown>, after: () => void) => {
        setError(null);

        try {
            await action();

            after();
        } catch (failure) {
            setError(failure);
        }
    };

    const addMember = (event: FormEvent) => {
        event.preventDefault();

        run(() => addBlockMember(organization, id, memberIsu), () => {
            setMemberIsu("");
            members.reload();
        });
    };

    const setManager = (event: FormEvent) => {
        event.preventDefault();

        run(() => assignBlockManager(organization, id, managerIsu), () => {
            setManagerIsu("");
            block.reload();
        });
    };

    if (block.loading) {
        return <Loader />;
    }

    if (!block.data) {
        return <ErrorMessage error={block.error} />;
    }

    return (
        <>
            <p className="muted">
                <Link to={`/organizations/${organization}`}>← к организации</Link>
            </p>

            <h1>{block.data.name}</h1>

            <section className="card">
                <dl className="facts">
                    <dt>Статус</dt>
                    <dd>{block.data.status}</dd>

                    <dt>Руководитель блока</dt>
                    <dd>{block.data.managerIsuNumber ?? "не назначен"}</dd>
                </dl>

                <ErrorMessage error={error} />

                <form className="row" onSubmit={setManager}>
                    <div>
                        <label htmlFor="blockManagerIsu">
                            Назначить руководителя блока (номер ИСУ)
                        </label>
                        <input
                            id="blockManagerIsu"
                            value={managerIsu}
                            onChange={(event) => setManagerIsu(event.target.value)}
                        />
                    </div>

                    <button type="submit" className="secondary">
                        Назначить
                    </button>
                </form>
            </section>

            <section className="card">
                <h2>Состав блока</h2>

                <ErrorMessage error={members.error} />

                <table>
                    <thead>
                        <tr>
                            <th>Участник</th>
                            <th>Номер ИСУ</th>
                            <th>Почта</th>
                            <th />
                        </tr>
                    </thead>
                    <tbody>
                        {members.data?.map((member) => (
                            <tr key={member.participantId}>
                                <td>
                                    {member.lastName} {member.firstName}
                                </td>
                                <td className="muted">{member.isuNumber}</td>
                                <td className="muted">{member.email}</td>
                                <td>
                                    <button
                                        type="button"
                                        className="link"
                                        onClick={() =>
                                            run(
                                                () =>
                                                    removeBlockMember(
                                                        organization,
                                                        id,
                                                        member.isuNumber
                                                    ),
                                                members.reload
                                            )
                                        }
                                    >
                                        убрать
                                    </button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>

                <form className="row" onSubmit={addMember}>
                    <div>
                        <label htmlFor="memberIsu">Добавить участника (номер ИСУ)</label>
                        <input
                            id="memberIsu"
                            value={memberIsu}
                            onChange={(event) => setMemberIsu(event.target.value)}
                        />
                    </div>

                    <button type="submit" className="secondary">
                        Добавить
                    </button>
                </form>
            </section>
        </>
    );
}
