create table invitations (
    id bigserial primary key,
    isu_number varchar(6) not null,
    email varchar(255) not null,
    organization_id bigint references organizations(id),
    participant_id bigint references organization_participants(id),
    source varchar(20) not null,
    token_hash varchar(64) not null,
    expires_at timestamp not null,
    used_at timestamp,
    created_by_user_id bigint references users(id),
    created_at timestamp not null default current_timestamp,

    constraint chk_invitations_isu_format
        check (isu_number ~ '^[1-9][0-9]{5}$'),

    constraint chk_invitations_source
        check (source in ('SELF_SERVICE', 'MANAGER_INVITE')),

    constraint uq_invitations_token_hash
        unique (token_hash)
);

create index idx_invitations_isu_number
    on invitations(isu_number);

create index idx_invitations_organization
    on invitations(organization_id);
