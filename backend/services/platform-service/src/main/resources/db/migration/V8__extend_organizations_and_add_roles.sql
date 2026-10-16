alter table organizations
    add column code varchar(50),
    add column status varchar(20) not null default 'ACTIVE',
    add column updated_at timestamp not null default current_timestamp;

update organizations
set code = 'org-' || id
where code is null;

alter table organizations
    alter column code set not null;

alter table organizations
    add constraint uq_organizations_code
        unique (code);

alter table organizations
    add constraint chk_organizations_code_format
        check (code ~ '^[a-z0-9][a-z0-9-]{1,49}$');

alter table organizations
    add constraint chk_organizations_status
        check (status in ('ACTIVE', 'INACTIVE'));

create table organization_roles (
    id bigserial primary key,
    organization_id bigint not null references organizations(id),
    user_id bigint not null references users(id),
    role varchar(30) not null,
    granted_at timestamp not null default current_timestamp,
    granted_by_user_id bigint references users(id),

    constraint chk_organization_roles_role
        check (role in ('MANAGER', 'ADMIN')),

    constraint uq_organization_roles_scope_user_role
        unique (organization_id, user_id, role)
);

create unique index uq_organization_roles_single_manager
    on organization_roles(organization_id)
    where role = 'MANAGER';

create index idx_organization_roles_user
    on organization_roles(user_id);
