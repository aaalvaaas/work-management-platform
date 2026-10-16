create table registration_requests (
    id bigserial primary key,
    organization_id bigint not null references organizations(id),
    isu_number varchar(6) not null,
    last_name varchar(100) not null,
    first_name varchar(100) not null,
    patronymic varchar(100),
    email varchar(255) not null,
    position varchar(255),
    password_hash varchar(255) not null,
    status varchar(20) not null,
    decision_comment varchar(500),
    created_at timestamp not null default current_timestamp,
    decided_at timestamp,
    decided_by_user_id bigint references users(id),

    constraint chk_registration_requests_status
        check (status in ('PENDING', 'APPROVED', 'REJECTED'))
);

create index idx_registration_requests_organization
    on registration_requests(organization_id);

create unique index uq_registration_requests_pending
    on registration_requests(organization_id, isu_number)
    where status = 'PENDING';
