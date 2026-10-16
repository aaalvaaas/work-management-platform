import { api } from "./client";
import type { Organization, OrganizationRole, Participant } from "./types";

export const listOrganizations = () => api.get<Organization[]>("/organizations");

export const getOrganization = (organizationId: number) =>
    api.get<Organization>(`/organizations/${organizationId}`);

export const createOrganization = (code: string, name: string) =>
    api.post<Organization>("/organizations", { code, name });

export const renameOrganization = (organizationId: number, name: string) =>
    api.patch<Organization>(`/organizations/${organizationId}`, { name });

export const assignManager = (organizationId: number, isuNumber: string) =>
    api.put<OrganizationRole>(`/organizations/${organizationId}/manager`, { isuNumber });

export const listRoles = (organizationId: number) =>
    api.get<OrganizationRole[]>(`/organizations/${organizationId}/roles`);

export const grantAdmin = (organizationId: number, isuNumber: string) =>
    api.put<OrganizationRole>(`/organizations/${organizationId}/admins`, { isuNumber });

export const revokeAdmin = (organizationId: number, isuNumber: string) =>
    api.remove<void>(`/organizations/${organizationId}/admins/${isuNumber}`);

export const listParticipants = (organizationId: number) =>
    api.get<Participant[]>(`/organizations/${organizationId}/participants`);

export const excludeParticipant = (organizationId: number, isuNumber: string) =>
    api.remove<Participant>(`/organizations/${organizationId}/participants/${isuNumber}`);
