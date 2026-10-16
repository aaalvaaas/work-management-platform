import { api } from "./client";
import type {
    Invitation,
    RegistrationDetails,
    RegistrationRequest,
    RegistrationSubmission,
    User
} from "./types";

export const submitRegistrationRequest = (submission: RegistrationSubmission) =>
    api.post<RegistrationRequest>("/registration/requests", submission);

export const registrationDetails = (token: string) =>
    api.get<RegistrationDetails>(`/registration/${token}`);

export const completeRegistration = (token: string, password: string) =>
    api.post<User>("/registration/complete", { token, password });

export const listRegistrationRequests = (organizationId: number) =>
    api.get<RegistrationRequest[]>(
        `/organizations/${organizationId}/registration-requests?status=PENDING`
    );

export const approveRegistrationRequest = (organizationId: number, requestId: number) =>
    api.post<RegistrationRequest>(
        `/organizations/${organizationId}/registration-requests/${requestId}/approve`
    );

export const rejectRegistrationRequest = (
    organizationId: number,
    requestId: number,
    comment: string
) =>
    api.post<RegistrationRequest>(
        `/organizations/${organizationId}/registration-requests/${requestId}/reject`,
        { comment }
    );

export const inviteParticipant = (organizationId: number, isuNumber: string) =>
    api.post<Invitation>(`/organizations/${organizationId}/invitations`, { isuNumber });
