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
