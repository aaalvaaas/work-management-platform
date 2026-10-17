alter table deck_boards
    alter column deck_board_id drop not null;

alter table deck_cards
    alter column deck_board_id drop not null,
    alter column deck_stack_id drop not null,
    alter column deck_card_id drop not null;
