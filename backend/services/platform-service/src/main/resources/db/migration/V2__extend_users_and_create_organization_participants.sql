create type user_status as enum
(
    'ACTIVE',
    'BLOCKED'
);

create type participant_status as enum
(
    'ACTIVE',
    'INACTIVE'
);

create table organizations
(
    id bigserial primary key,
    name varchar(255) not null,
    status varchar(20) not null default 'ACTIVE',
    created_at timestamp not null default current_timestamp,

    constraint chk_organizations_status check (status in ('ACTIVE', 'INACTIVE'))
)

alter table users
    add column isu_number varchar(6),
    add column password_hash varchar(255),
    add column first_name varchar(100),
    add column last_name varchar(100),
    add column patronymic varchar(100),
    add column position varchar(255),
    add column status user_status not null default 'ACTIVE',
    add column created_at timestamp not null default current_timestamp;

alter table users
    add constraint chk_users_isu_format
    check (
        isu_number is null
        or isu_number ~ '^[1-9][0-9]{5}$'
    );

create unique index uq_users_isu_number
    on users(isu_number) where isu_number is not null;

create table organization_participants
(
    id bigserial primary key,
    organization_id bigint not null references organizations(id),
    isu_number varchar(6) not null,
    first_name varchar(100) not null,
    last_name varchar(100) not null,
    patronymic varchar(100),
    email varchar(255) not null,
    position varchar(255),
    status participant_status not null default 'ACTIVE',
    user_id bigint references user(id),
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,

    constraint chk_participants_isu_format check (isu_number ~ '^[1-9][0-9]{5}$'),

    constraint uq_participants_org_isu unique (organization_id, isu_number)
);

create index idx_organization_participants_org on organization_participants(organization_id);

create index idx_organization_participants_user on organization_participants(user_id);

create index idx_organizations_participants_email on organization_participants(email);
