import { useAuth } from "../auth/AuthContext";

export function ProfilePage() {
    const { user, isPlatformAdmin } = useAuth();

    if (!user) {
        return null;
    }

    return (
        <section className="card">
            <h1>Профиль</h1>

            <dl className="facts">
                <dt>Фамилия и имя</dt>
                <dd>
                    {user.lastName} {user.firstName} {user.patronymic ?? ""}
                </dd>

                <dt>Номер ИСУ</dt>
                <dd>{user.isuNumber}</dd>

                <dt>Почта</dt>
                <dd>{user.email}</dd>

                <dt>Должность</dt>
                <dd>{user.position ?? "не указана"}</dd>

                <dt>Права</dt>
                <dd>{isPlatformAdmin ? "администратор платформы" : "пользователь"}</dd>
            </dl>
        </section>
    );
}
