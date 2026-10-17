import { useCallback, useEffect, useState } from "react";
import type { FormEvent } from "react";
import { connectMailbox, disableMailbox, disconnectMailbox, getMailbox } from "../api/mailbox";
import { ApiError } from "../api/client";
import type { MailboxSettings } from "../api/types";
import { ErrorMessage } from "./ErrorMessage";
import { useResource } from "./useResource";

const EMPTY: MailboxSettings = {
    host: "",
    port: 993,
    username: "",
    password: "",
    folder: "INBOX",
    useSsl: true
};

export function OrganizationMailbox({ organizationId }: { organizationId: number }) {
    const mailbox = useResource(
        useCallback(() => getMailbox(organizationId), [organizationId]),
        [organizationId]
    );

    const [settings, setSettings] = useState<MailboxSettings>(EMPTY);
    const [error, setError] = useState<unknown>(null);

    useEffect(() => {
        if (mailbox.data) {
            setSettings({
                host: mailbox.data.host,
                port: mailbox.data.port,
                username: mailbox.data.username,
                password: "",
                folder: mailbox.data.folder,
                useSsl: mailbox.data.useSsl
            });
        }
    }, [mailbox.data]);

    const missing = mailbox.error instanceof ApiError && mailbox.error.code === "MAILBOX_NOT_FOUND";

    const run = async (action: () => Promise<unknown>) => {
        setError(null);

        try {
            await action();

            mailbox.reload();
        } catch (failure) {
            setError(failure);
        }
    };

    const submit = (event: FormEvent) => {
        event.preventDefault();

        run(() => connectMailbox(organizationId, settings));
    };

    return (
        <section className="card">
            <h2>Почтовый ящик организации</h2>

            <p className="muted">
                Письма из этого ящика система разбирает в задачи. Пароль хранится в зашифрованном
                виде и обратно не показывается.
            </p>

            {mailbox.data && (
                <dl className="facts">
                    <dt>Состояние</dt>
                    <dd>
                        <span className="tag">{mailbox.data.status}</span>
                        {mailbox.data.lastError ? ` ${mailbox.data.lastError}` : ""}
                    </dd>

                    <dt>Последняя проверка</dt>
                    <dd>{mailbox.data.lastCheckedAt ?? "ещё не было"}</dd>
                </dl>
            )}

            {!missing && <ErrorMessage error={mailbox.error} />}
            <ErrorMessage error={error} />

            <form onSubmit={submit}>
                <div className="row">
                    <div>
                        <label htmlFor="mailboxHost">Сервер IMAP</label>
                        <input
                            id="mailboxHost"
                            value={settings.host}
                            onChange={(event) =>
                                setSettings({ ...settings, host: event.target.value })
                            }
                        />
                    </div>

                    <div>
                        <label htmlFor="mailboxPort">Порт</label>
                        <input
                            id="mailboxPort"
                            inputMode="numeric"
                            value={settings.port}
                            onChange={(event) =>
                                setSettings({ ...settings, port: Number(event.target.value) })
                            }
                        />
                    </div>
                </div>

                <div className="row">
                    <div>
                        <label htmlFor="mailboxUsername">Адрес или логин</label>
                        <input
                            id="mailboxUsername"
                            value={settings.username}
                            onChange={(event) =>
                                setSettings({ ...settings, username: event.target.value })
                            }
                        />
                    </div>

                    <div>
                        <label htmlFor="mailboxPassword">
                            Пароль {mailbox.data ? "(оставьте пустым, чтобы не менять)" : ""}
                        </label>
                        <input
                            id="mailboxPassword"
                            type="password"
                            autoComplete="new-password"
                            value={settings.password}
                            onChange={(event) =>
                                setSettings({ ...settings, password: event.target.value })
                            }
                        />
                    </div>

                    <div>
                        <label htmlFor="mailboxFolder">Папка</label>
                        <input
                            id="mailboxFolder"
                            value={settings.folder}
                            onChange={(event) =>
                                setSettings({ ...settings, folder: event.target.value })
                            }
                        />
                    </div>
                </div>

                <div className="row">
                    <button type="submit">
                        {mailbox.data ? "Сохранить" : "Подключить ящик"}
                    </button>

                    {mailbox.data && (
                        <>
                            <button
                                type="button"
                                className="secondary"
                                onClick={() => run(() => disableMailbox(organizationId))}
                            >
                                Приостановить
                            </button>

                            <button
                                type="button"
                                className="danger"
                                onClick={() => run(() => disconnectMailbox(organizationId))}
                            >
                                Отключить
                            </button>
                        </>
                    )}
                </div>
            </form>
        </section>
    );
}
