create table block_members (
    id bigserial primary key,
    block_id bigint not null references blocks(id),
    participant_id bigint not null references organization_participants(id),
    added_at timestamp not null default current_timestamp,
    added_by_user_id bigint references users(id),

    constraint uq_block_members_block_participant
        unique (block_id, participant_id)
);

create index idx_block_members_participant
    on block_members(participant_id);
