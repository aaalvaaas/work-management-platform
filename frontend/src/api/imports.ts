import { api } from "./client";
import type { ImportBatch, ImportPreview } from "./types";

export const uploadParticipantList = (organizationId: number, file: File) =>
    api.upload<ImportPreview>(`/organizations/${organizationId}/imports`, file);

export const listImportBatches = (organizationId: number) =>
    api.get<ImportBatch[]>(`/organizations/${organizationId}/imports`);

export const getImportPreview = (organizationId: number, batchId: number) =>
    api.get<ImportPreview>(`/organizations/${organizationId}/imports/${batchId}`);

export const applyImport = (organizationId: number, batchId: number) =>
    api.post<ImportBatch>(`/organizations/${organizationId}/imports/${batchId}/apply`);

export const cancelImport = (organizationId: number, batchId: number) =>
    api.post<ImportBatch>(`/organizations/${organizationId}/imports/${batchId}/cancel`);
