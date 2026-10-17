import { useCallback, useState } from "react";
import type { FormEvent } from "react";
import {
    approveRegistrationRequest,
    inviteParticipant,
    listRegistrationRequests,
    rejectRegistrationRequest
} from "../api/registration";
import { ErrorMessage } from "../components/ErrorMessage";
import { Loader } from "../components/Loader";
import { useResource } from "../components/useResource";

interface RegistrationRequestsProps {
    organizationId: number;
    onDecision?: () => void;
}

export function RegistrationRequests({ organizationId, onDecision }: RegistrationRequestsProps) {
    const requests = useResource(
        useCallback(() => listRegistrationRequests(organizationId), [organizationId]),
        [organizationId]
    );

    const [error, setError] = useState<unknown>(null);
    const [rejectingId, setRejectingId] = useState<number | null>(null);
    const [comment, setComment] = useState("");
    const [inviteIsu, setInviteIsu] = useState("");
    const [invitationLink, setInvitationLink] = useState<string | null>(null);

    const run = async (action: () => Promise<unknown>) => {
        setError(null);

        try {
            await action();

            requests.reload();
            onDecision?.();
        } catch (failure) {
            setError(failure);
        }
    };

    const approve = (requestId: number) =>
        run(() => approveRegistrationRequest(organizationId, requestId));

    const reject = (requestId: number) =>
        run(async () => {
            await rejectRegistrationRequest(organizationId, requestId, comment);

            setRejectingId(null);
            setComment("");
        });

    const invite = (event: FormEvent) => {
        event.preventDefault();
        setError(null);
        setInvitationLink(null);

        inviteParticipant(organizationId, inviteIsu)
            .then((invitation) => {
                setInvitationLink(
                    `${window.location.origin}/registration/${invitation.registrationToken}`
                );
                setInviteIsu("");
            })
            .catch((failure: unknown) => setError(failure));
    };

    return (
        <section className="card">
            <h2>Заявки на регистрацию</h2>

            {requests.loading && <Loader />}
            <ErrorMessage error={error} />
            <ErrorMessage error={requests.error} />

            {requests.data?.length === 0 && <p className="muted">Заявок нет.</p>}

            {requests.data && requests.data.length > 0 && (
                <table>
                    <thead>
                        <tr>
                            <th>Человек</th>
                            <th>Номер ИСУ</th>
                            <th>Почта</th>
                            <th>Должность</th>
                            <th />
                        </tr>
                    </thead>
                    <tbody>
                        {requests.data.map((request) => (
                            <tr key={request.id}>
                                <td>
                                    {request.lastName} {request.firstName}{" "}
                                    {request.patronymic ?? ""}
                                </td>
                                <td className="muted">{request.isuNumber}</td>
                                <td className="muted">{request.email}</td>
                                <td className="muted">{request.position ?? ""}</td>
                                <td>
                                    {rejectingId === request.id ? (
                                        <span className="row">
                                            <input
                                                aria-label="Причина отказа"
                                                value={comment}
                                                onChange={(event) =>
                                                    setComment(event.target.value)
                                                }
                                            />
                                            <button
                                                type="button"
                                                className="link"
                                                onClick={() => reject(request.id)}
                                            >
                                                подтвердить
                                            </button>
                                        </span>
                                    ) : (
                                        <span className="row">
                                            <button
                                                type="button"
                                                className="link"
                                                onClick={() => approve(request.id)}
                                            >
                                                одобрить
                                            </button>
                                            <button
                                                type="button"
                                                className="link"
                                                onClick={() => setRejectingId(request.id)}
                                            >
                                                отклонить
                                            </button>
                                        </span>
                                    )}
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            )}

            <form className="row" onSubmit={invite}>
                <div>
                    <label htmlFor="inviteIsu">Пригласить участника (номер ИСУ)</label>
                    <input
                        id="inviteIsu"
                        value={inviteIsu}
                        onChange={(event) => setInviteIsu(event.target.value)}
                    />
                </div>

                <button type="submit" className="secondary">
                    Выдать ссылку
                </button>
            </form>

            {invitationLink && (
                <>
                    <label htmlFor="invitationLink">
                        Ссылка для регистрации, передайте её человеку
                    </label>
                    <input id="invitationLink" readOnly value={invitationLink} />
                </>
            )}
        </section>
    );
}
