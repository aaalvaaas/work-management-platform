create table import_batches (
    id bigserial primary key,
    organization_id bigint not null references organizations(id),
    filename varchar(255) not null,
    status varchar(20) not null,
    total_rows integer not null default 0,
    error_count integer not null default 0,
    created_by_user_id bigint references users(id),
    created_at timestamp not null default current_timestamp,
    applied_at timestamp,
    applied_by_user_id bigint references users(id),

    constraint chk_import_batches_status
        check (status in ('VALIDATED', 'FAILED', 'APPLIED', 'CANCELLED'))
);

create index idx_import_batches_organization
    on import_batches(organization_id);

create table import_changes (
    id bigserial primary key,
    batch_id bigint not null references import_batches(id) on delete cascade,
    row_number integer,
    type varchar(20) not null,
    isu_number varchar(6),
    last_name varchar(100),
    first_name varchar(100),
    patronymic varchar(100),
    email varchar(255),
    position varchar(255),
    block_names varchar(1000),
    details varchar(1000),
    participant_id bigint references organization_participants(id),

    constraint chk_import_changes_type
        check (type in ('CREATE', 'UPDATE', 'DEACTIVATE', 'UNCHANGED', 'ERROR'))
);

create index idx_import_changes_batch
    on import_changes(batch_id);
