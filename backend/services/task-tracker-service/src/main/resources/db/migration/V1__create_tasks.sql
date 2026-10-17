create table tasks (
    id bigserial primary key,
    organization_id bigint not null,
    block_id bigint,
    title varchar(255) not null,
    description text,
    status varchar(20) not null,
    requires_approval boolean not null default false,
    source varchar(20) not null,
    source_reference varchar(255),
    author_user_id bigint not null,
    assignee_user_id bigint,
    assignee_isu_number varchar(6),
    due_date date,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,

    constraint chk_tasks_status
        check (status in ('NEW', 'IN_PROGRESS', 'ON_APPROVAL', 'DONE', 'CANCELLED')),

    constraint chk_tasks_source
        check (source in ('MANUAL', 'MAIL'))
);

create index idx_tasks_organization on tasks(organization_id);

create index idx_tasks_block on tasks(block_id);

create index idx_tasks_assignee on tasks(assignee_user_id);
