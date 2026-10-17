create table organization_mailboxes (
    id bigserial primary key,
    organization_id bigint not null references organizations(id),
    host varchar(255) not null,
    port integer not null,
    username varchar(255) not null,
    password_cipher text not null,
    folder varchar(100) not null default 'INBOX',
    use_ssl boolean not null default true,
    status varchar(20) not null,
    last_error varchar(500),
    last_checked_at timestamp,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,

    constraint uq_organization_mailboxes_organization unique (organization_id),

    constraint chk_organization_mailboxes_status
        check (status in ('ACTIVE', 'DISABLED', 'ERROR'))
);
