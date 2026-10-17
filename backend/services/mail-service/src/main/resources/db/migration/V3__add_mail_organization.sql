alter table mail_messages
    add column organization_id bigint,
    add column mailbox_id bigint;

create index idx_mail_messages_organization on mail_messages(organization_id);
