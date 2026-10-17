import { api } from "./client";
import type { Mailbox, MailboxSettings } from "./types";

export const getMailbox = (organizationId: number) =>
    api.get<Mailbox>(`/organizations/${organizationId}/mailbox`);

export const connectMailbox = (organizationId: number, settings: MailboxSettings) =>
    api.put<Mailbox>(`/organizations/${organizationId}/mailbox`, settings);

export const disableMailbox = (organizationId: number) =>
    api.post<Mailbox>(`/organizations/${organizationId}/mailbox/disable`);

export const disconnectMailbox = (organizationId: number) =>
    api.remove<void>(`/organizations/${organizationId}/mailbox`);
