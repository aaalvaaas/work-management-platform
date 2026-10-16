create table blocks (
    id bigserial primary key,
    organization_id bigint not null references organizations(id),
    name varchar(255) not null,
    status varchar(20) not null default 'ACTIVE',
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,

    constraint chk_blocks_status
        check (status in ('ACTIVE', 'INACTIVE')),

    constraint uq_blocks_organization_name
        unique (organization_id, name)
);

create index idx_blocks_organization
    on blocks(organization_id);

create table block_roles (
    id bigserial primary key,
    block_id bigint not null references blocks(id),
    user_id bigint not null references users(id),
    role varchar(30) not null,
    granted_at timestamp not null default current_timestamp,
    granted_by_user_id bigint references users(id),

    constraint chk_block_roles_role
        check (role in ('MANAGER')),

    constraint uq_block_roles_scope_user_role
        unique (block_id, user_id, role)
);

create unique index uq_block_roles_single_manager
    on block_roles(block_id)
    where role = 'MANAGER';

create index idx_block_roles_user
    on block_roles(user_id);
