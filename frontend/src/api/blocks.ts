import { api } from "./client";
import type { Block, BlockMember } from "./types";

export const listBlocks = (organizationId: number) =>
    api.get<Block[]>(`/organizations/${organizationId}/blocks`);

export const getBlock = (organizationId: number, blockId: number) =>
    api.get<Block>(`/organizations/${organizationId}/blocks/${blockId}`);

export const createBlock = (organizationId: number, name: string) =>
    api.post<Block>(`/organizations/${organizationId}/blocks`, { name });

export const assignBlockManager = (organizationId: number, blockId: number, isuNumber: string) =>
    api.put<unknown>(`/organizations/${organizationId}/blocks/${blockId}/manager`, { isuNumber });

export const listBlockMembers = (organizationId: number, blockId: number) =>
    api.get<BlockMember[]>(`/organizations/${organizationId}/blocks/${blockId}/members`);

export const addBlockMember = (organizationId: number, blockId: number, isuNumber: string) =>
    api.post<BlockMember>(`/organizations/${organizationId}/blocks/${blockId}/members`, {
        isuNumber
    });

export const removeBlockMember = (organizationId: number, blockId: number, isuNumber: string) =>
    api.remove<void>(`/organizations/${organizationId}/blocks/${blockId}/members/${isuNumber}`);
