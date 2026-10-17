create table deck_boards (
    id bigserial primary key,
    organization_id bigint not null,
    block_id bigint,
    deck_board_id bigint not null,
    created_at timestamp not null default current_timestamp
);

create unique index uq_deck_boards_block
    on deck_boards(organization_id, block_id)
    where block_id is not null;

create unique index uq_deck_boards_organization
    on deck_boards(organization_id)
    where block_id is null;

create table deck_stacks (
    id bigserial primary key,
    board_id bigint not null references deck_boards(id),
    status varchar(20) not null,
    deck_stack_id bigint not null,

    constraint uq_deck_stacks_status unique (board_id, status)
);

create table deck_cards (
    id bigserial primary key,
    task_id bigint not null references tasks(id),
    deck_board_id bigint not null,
    deck_stack_id bigint not null,
    deck_card_id bigint not null,
    sync_status varchar(20) not null,
    last_error varchar(500),
    deck_last_modified bigint,
    last_synced_at timestamp,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,

    constraint uq_deck_cards_task unique (task_id),

    constraint chk_deck_cards_sync_status
        check (sync_status in ('PENDING', 'SYNCED', 'FAILED', 'DETACHED'))
);

create index idx_deck_cards_sync_status on deck_cards(sync_status);
