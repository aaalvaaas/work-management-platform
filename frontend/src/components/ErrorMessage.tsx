import { ApiError } from "../api/client";

export function ErrorMessage({ error }: { error: unknown }) {
    if (!error) {
        return null;
    }

    return (
        <p className="error" role="alert">
            {describe(error)}
        </p>
    );
}

export function describe(error: unknown): string {
    if (error instanceof ApiError) {
        const fields = Object.values(error.fieldErrors);

        if (fields.length > 0) {
            return fields.join("; ");
        }

        return error.message || error.code;
    }

    if (error instanceof Error) {
        return error.message;
    }

    return "Непредвиденная ошибка";
}
