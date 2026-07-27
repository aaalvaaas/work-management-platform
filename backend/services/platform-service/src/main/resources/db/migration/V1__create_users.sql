create table users
(
    id uuid primary key,
    email varchar(255) not null unique,
    username varchar(100) not null,
    created_at timestamp not null default now()
)
