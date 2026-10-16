import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "./AuthContext";
import { Loader } from "../components/Loader";

export function RequireAuth() {
    const { status } = useAuth();
    const location = useLocation();

    if (status === "loading") {
        return <Loader label="Проверяем сессию" />;
    }

    if (status === "anonymous") {
        return <Navigate to="/login" state={{ from: location.pathname }} replace />;
    }

    return <Outlet />;
}
