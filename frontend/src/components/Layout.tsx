import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

export function Layout() {
    const { user, logout } = useAuth();
    const navigate = useNavigate();

    const signOut = async () => {
        await logout();

        navigate("/login", { replace: true });
    };

    return (
        <div className="shell">
            <header className="top">
                <span className="brand">WMP</span>

                <nav>
                    <NavLink to="/organizations">Организации</NavLink>
                    <NavLink to="/profile">Профиль</NavLink>
                </nav>

                <div className="session">
                    <span className="muted">{user ? `${user.lastName} ${user.firstName}` : ""}</span>
                    <button type="button" className="link" onClick={signOut}>
                        Выйти
                    </button>
                </div>
            </header>

            <main>
                <Outlet />
            </main>
        </div>
    );
}
