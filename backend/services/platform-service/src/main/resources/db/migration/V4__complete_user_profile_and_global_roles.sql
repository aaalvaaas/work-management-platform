alter table users
    drop column username;

alter table users
    add column updated_at timestamp not null default current_timestamp;

alter table users
    alter column isu_number set not null,
    alter column first_name set not null,
    alter column last_name set not null,
    alter column password_hash set not null;

drop index uq_users_isu_number;

alter table users
    add constraint uq_users_isu_number
        unique (isu_number);

alter table users
    drop constraint chk_users_isu_format;

alter table users
    add constraint chk_users_isu_format
        check (isu_number ~ '^[1-9][0-9]{5}$');

create table user_global_roles (
    id bigserial primary key,
    user_id bigint not null references users(id),
    role varchar(30) not null,
    granted_at timestamp not null default current_timestamp,
    granted_by_user_id bigint references users(id),

    constraint chk_user_global_roles_role
        check (role in ('PLATFORM_ADMIN')),

    constraint uq_user_global_roles_user_role
        unique (user_id, role)
);

create index idx_user_global_roles_user
    on user_global_roles(user_id);
