create table users
(
    id bigserial primary key,
    username varchar(100) not null,
    email varchar(255) not null unique
);
