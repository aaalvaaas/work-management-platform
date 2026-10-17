export type UserStatus = "ACTIVE" | "BLOCKED";

export type GlobalRole = "PLATFORM_ADMIN";

export type OrganizationStatus = "ACTIVE" | "INACTIVE";

export type BlockStatus = "ACTIVE" | "INACTIVE";

export type ParticipantStatus = "ACTIVE" | "INACTIVE";

export type OrganizationRoleType = "MANAGER" | "ADMIN";

export type ImportBatchStatus = "VALIDATED" | "FAILED" | "APPLIED" | "CANCELLED";

export type ImportChangeType = "CREATE" | "UPDATE" | "DEACTIVATE" | "UNCHANGED" | "ERROR";

export interface TokenResponse {
    accessToken: string;
    tokenType: string;
    expiresIn: number;
}

export interface User {
    id: number;
    isuNumber: string;
    lastName: string;
    firstName: string;
    patronymic: string | null;
    email: string;
    position: string | null;
    status: UserStatus;
    globalRoles: GlobalRole[];
    createdAt: string;
}

export interface Organization {
    id: number;
    code: string;
    name: string;
    status: OrganizationStatus;
    managerIsuNumber: string | null;
    createdAt: string;
}

export interface OrganizationRole {
    userId: number;
    isuNumber: string;
    fullName: string;
    role: OrganizationRoleType;
    grantedAt: string;
}

export interface Participant {
    id: number;
    isuNumber: string;
    lastName: string;
    firstName: string;
    patronymic: string | null;
    email: string;
    position: string | null;
    status: ParticipantStatus;
    registered: boolean;
}

export interface Block {
    id: number;
    organizationId: number;
    name: string;
    status: BlockStatus;
    managerIsuNumber: string | null;
    createdAt: string;
}

export interface BlockMember {
    participantId: number;
    isuNumber: string;
    lastName: string;
    firstName: string;
    patronymic: string | null;
    email: string;
    position: string | null;
    registered: boolean;
    addedAt: string;
}

export interface ImportBatch {
    id: number;
    organizationId: number;
    filename: string;
    status: ImportBatchStatus;
    totalRows: number;
    created: number;
    updated: number;
    deactivated: number;
    unchanged: number;
    errors: number;
    createdAt: string;
    appliedAt: string | null;
}

export interface ImportChange {
    rowNumber: number | null;
    type: ImportChangeType;
    isuNumber: string | null;
    lastName: string | null;
    firstName: string | null;
    patronymic: string | null;
    email: string | null;
    position: string | null;
    blockNames: string | null;
    details: string | null;
}

export interface ImportPreview {
    batch: ImportBatch;
    changes: ImportChange[];
}

export type RegistrationRequestStatus = "PENDING" | "APPROVED" | "REJECTED";

export interface RegistrationRequest {
    id: number;
    organizationId: number;
    organizationName: string;
    isuNumber: string;
    lastName: string;
    firstName: string;
    patronymic: string | null;
    email: string;
    position: string | null;
    status: RegistrationRequestStatus;
    decisionComment: string | null;
    createdAt: string;
    decidedAt: string | null;
}

export interface RegistrationSubmission {
    organizationCode: string;
    isuNumber: string;
    lastName: string;
    firstName: string;
    patronymic: string;
    email: string;
    position: string;
    password: string;
}

export interface RegistrationDetails {
    isuNumber: string;
    lastName: string;
    firstName: string;
    patronymic: string | null;
    email: string;
    organizations: string[];
}

export interface Invitation {
    id: number;
    isuNumber: string;
    email: string;
    organizationId: number;
    source: string;
    expiresAt: string;
    registrationToken: string;
}

export type TaskStatus = "NEW" | "IN_PROGRESS" | "ON_APPROVAL" | "DONE" | "CANCELLED";

export type TaskSource = "MANUAL" | "MAIL";

export interface Task {
    id: number;
    organizationId: number;
    blockId: number | null;
    title: string;
    description: string | null;
    status: TaskStatus;
    requiresApproval: boolean;
    source: TaskSource;
    authorUserId: number;
    assigneeUserId: number | null;
    assigneeIsuNumber: string | null;
    dueDate: string | null;
    createdAt: string;
    updatedAt: string;
}

export interface NewTask {
    organizationId: number;
    blockId: number | null;
    title: string;
    description: string;
    assigneeIsuNumber: string | null;
    dueDate: string | null;
    requiresApproval: boolean;
}

export type MailboxStatus = "ACTIVE" | "DISABLED" | "ERROR";

export interface Mailbox {
    id: number;
    organizationId: number;
    host: string;
    port: number;
    username: string;
    folder: string;
    useSsl: boolean;
    status: MailboxStatus;
    lastError: string | null;
    lastCheckedAt: string | null;
}

export interface MailboxSettings {
    host: string;
    port: number;
    username: string;
    password: string;
    folder: string;
    useSsl: boolean;
}
