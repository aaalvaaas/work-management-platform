create table refresh_tokens (
    id bigserial primary key,
    user_id bigint not null references users(id),
    token_hash varchar(64) not null,
    issued_at timestamp not null default current_timestamp,
    expires_at timestamp not null,
    revoked_at timestamp,

    constraint uq_refresh_tokens_token_hash
        unique (token_hash)
);

create index idx_refresh_tokens_user
    on refresh_tokens(user_id);
