export function Loader({ label = "Загружаем" }: { label?: string }) {
    return <p className="muted">{label}…</p>;
}
