import { useCallback, useState } from "react";
import type { ChangeEvent } from "react";
import { Link, useParams } from "react-router-dom";
import {
    applyImport,
    cancelImport,
    getImportPreview,
    listImportBatches,
    uploadParticipantList
} from "../api/imports";
import type { ImportChangeType, ImportPreview } from "../api/types";
import { ErrorMessage } from "../components/ErrorMessage";
import { Loader } from "../components/Loader";
import { useResource } from "../components/useResource";

const CHANGE_LABELS: Record<ImportChangeType, string> = {
    CREATE: "добавить",
    UPDATE: "обновить",
    DEACTIVATE: "исключить",
    UNCHANGED: "без изменений",
    ERROR: "ошибка"
};

export function ImportPage() {
    const { organizationId } = useParams();
    const id = Number(organizationId);

    const history = useResource(useCallback(() => listImportBatches(id), [id]), [id]);

    const [file, setFile] = useState<File | null>(null);
    const [preview, setPreview] = useState<ImportPreview | null>(null);
    const [error, setError] = useState<unknown>(null);
    const [busy, setBusy] = useState(false);

    const run = async (action: () => Promise<unknown>) => {
        setBusy(true);
        setError(null);

        try {
            await action();
        } catch (failure) {
            setError(failure);
        } finally {
            setBusy(false);
        }
    };

    const chooseFile = (event: ChangeEvent<HTMLInputElement>) => {
        setFile(event.target.files?.[0] ?? null);
        setPreview(null);
    };

    const upload = () => {
        if (!file) {
            return;
        }

        run(async () => {
            setPreview(await uploadParticipantList(id, file));

            history.reload();
        });
    };

    const open = (batchId: number) =>
        run(async () => setPreview(await getImportPreview(id, batchId)));

    const apply = (batchId: number) =>
        run(async () => {
            await applyImport(id, batchId);

            setPreview(await getImportPreview(id, batchId));
            history.reload();
        });

    const cancel = (batchId: number) =>
        run(async () => {
            await cancelImport(id, batchId);

            setPreview(await getImportPreview(id, batchId));
            history.reload();
        });

    return (
        <>
            <p className="muted">
                <Link to={`/organizations/${id}`}>← к организации</Link>
            </p>

            <h1>Импорт участников</h1>

            <section className="card">
                <p className="muted">
                    Поддерживаются файлы CSV и XLSX. Колонки определяются по заголовкам: номер ИСУ,
                    фамилия, имя, отчество, почта, должность, блоки.
                </p>

                <div className="row">
                    <input type="file" accept=".csv,.xlsx" onChange={chooseFile} />

                    <button type="button" onClick={upload} disabled={!file || busy}>
                        Проверить файл
                    </button>
                </div>

                <ErrorMessage error={error} />
            </section>

            {preview && (
                <section className="card">
                    <h2>
                        {preview.batch.filename} <span className="tag">{preview.batch.status}</span>
                    </h2>

                    <dl className="facts">
                        <dt>Строк в файле</dt>
                        <dd>{preview.batch.totalRows}</dd>

                        <dt>Будет добавлено</dt>
                        <dd>{preview.batch.created}</dd>

                        <dt>Будет обновлено</dt>
                        <dd>{preview.batch.updated}</dd>

                        <dt>Будет исключено</dt>
                        <dd>{preview.batch.deactivated}</dd>

                        <dt>Без изменений</dt>
                        <dd>{preview.batch.unchanged}</dd>

                        <dt>Ошибок</dt>
                        <dd>{preview.batch.errors}</dd>
                    </dl>

                    {preview.batch.status === "VALIDATED" && (
                        <div className="row">
                            <button
                                type="button"
                                onClick={() => apply(preview.batch.id)}
                                disabled={busy}
                            >
                                Применить
                            </button>

                            <button
                                type="button"
                                className="danger"
                                onClick={() => cancel(preview.batch.id)}
                                disabled={busy}
                            >
                                Отменить
                            </button>
                        </div>
                    )}

                    {preview.batch.status === "FAILED" && (
                        <p className="error">
                            В файле есть ошибки, применить его нельзя. Исправьте строки и загрузите
                            заново.
                        </p>
                    )}

                    <table>
                        <thead>
                            <tr>
                                <th>Строка</th>
                                <th>Что будет</th>
                                <th>Номер ИСУ</th>
                                <th>Участник</th>
                                <th>Подробности</th>
                            </tr>
                        </thead>
                        <tbody>
                            {preview.changes.map((change, index) => (
                                <tr key={`${change.rowNumber ?? "x"}-${index}`}>
                                    <td className="muted">{change.rowNumber ?? ""}</td>
                                    <td>
                                        <span className="tag">{CHANGE_LABELS[change.type]}</span>
                                    </td>
                                    <td className="muted">{change.isuNumber ?? ""}</td>
                                    <td>
                                        {change.lastName ?? ""} {change.firstName ?? ""}
                                    </td>
                                    <td className={change.type === "ERROR" ? "error" : "muted"}>
                                        {change.details ?? ""}
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </section>
            )}

            <section className="card">
                <h2>История загрузок</h2>

                {history.loading && <Loader />}
                <ErrorMessage error={history.error} />

                {history.data?.length === 0 && <p className="muted">Загрузок ещё не было.</p>}

                {history.data && history.data.length > 0 && (
                    <table>
                        <thead>
                            <tr>
                                <th>Файл</th>
                                <th>Статус</th>
                                <th>Строк</th>
                                <th>Загружен</th>
                                <th />
                            </tr>
                        </thead>
                        <tbody>
                            {history.data.map((batch) => (
                                <tr key={batch.id}>
                                    <td>{batch.filename}</td>
                                    <td>
                                        <span className="tag">{batch.status}</span>
                                    </td>
                                    <td className="muted">{batch.totalRows}</td>
                                    <td className="muted">{batch.createdAt}</td>
                                    <td>
                                        <button
                                            type="button"
                                            className="link"
                                            onClick={() => open(batch.id)}
                                        >
                                            показать
                                        </button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                )}
            </section>
        </>
    );
}
