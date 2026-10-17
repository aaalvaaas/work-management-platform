import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider } from "./auth/AuthContext";
import { RequireAuth } from "./auth/RequireAuth";
import { Layout } from "./components/Layout";
import { InvitationPage } from "./pages/InvitationPage";
import { LoginPage } from "./pages/LoginPage";
import { RegisterPage } from "./pages/RegisterPage";
import { OrganizationPage } from "./pages/OrganizationPage";
import { OrganizationsPage } from "./pages/OrganizationsPage";
import { BlockPage } from "./pages/BlockPage";
import { ImportPage } from "./pages/ImportPage";
import { ProfilePage } from "./pages/ProfilePage";
import { TasksPage } from "./pages/TasksPage";

export function App() {
    return (
        <BrowserRouter>
            <AuthProvider>
                <Routes>
                    <Route path="/login" element={<LoginPage />} />
                    <Route path="/register" element={<RegisterPage />} />
                    <Route path="/registration/:token" element={<InvitationPage />} />

                    <Route element={<RequireAuth />}>
                        <Route element={<Layout />}>
                            <Route path="/profile" element={<ProfilePage />} />
                            <Route path="/organizations" element={<OrganizationsPage />} />
                            <Route
                                path="/organizations/:organizationId"
                                element={<OrganizationPage />}
                            />
                            <Route
                                path="/organizations/:organizationId/blocks/:blockId"
                                element={<BlockPage />}
                            />
                            <Route
                                path="/organizations/:organizationId/import"
                                element={<ImportPage />}
                            />
                            <Route
                                path="/organizations/:organizationId/tasks"
                                element={<TasksPage />}
                            />
                        </Route>
                    </Route>

                    <Route path="*" element={<Navigate to="/organizations" replace />} />
                </Routes>
            </AuthProvider>
        </BrowserRouter>
    );
}
