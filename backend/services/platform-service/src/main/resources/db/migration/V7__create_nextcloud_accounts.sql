create table nextcloud_accounts (
    id bigserial primary key,
    user_id bigint not null references users(id),
    nextcloud_uid varchar(64) not null,
    status varchar(20) not null,
    last_error varchar(500),
    last_synced_at timestamp,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,

    constraint chk_nextcloud_accounts_status
        check (status in ('PENDING', 'ACTIVE', 'DISABLED', 'FAILED')),

    constraint uq_nextcloud_accounts_user
        unique (user_id),

    constraint uq_nextcloud_accounts_uid
        unique (nextcloud_uid)
);
