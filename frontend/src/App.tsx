import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider } from "./auth/AuthContext";
import { RequireAuth } from "./auth/RequireAuth";
import { Layout } from "./components/Layout";
import { LoginPage } from "./pages/LoginPage";
import { OrganizationPage } from "./pages/OrganizationPage";
import { OrganizationsPage } from "./pages/OrganizationsPage";
import { BlockPage } from "./pages/BlockPage";
import { ProfilePage } from "./pages/ProfilePage";

export function App() {
    return (
        <BrowserRouter>
            <AuthProvider>
                <Routes>
                    <Route path="/login" element={<LoginPage />} />

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
                        </Route>
                    </Route>

                    <Route path="*" element={<Navigate to="/organizations" replace />} />
                </Routes>
            </AuthProvider>
        </BrowserRouter>
    );
}
