alter table users
    alter column status drop default,
    alter column status type varchar(20) using status::text,
    alter column status set default 'ACTIVE';

alter table users
    add constraint chk_users_status
        check (status in ('ACTIVE', 'BLOCKED'));

alter table organization_participants
    alter column status drop default,
    alter column status type varchar(20) using status::text,
    alter column status set default 'ACTIVE';

alter table organization_participants
    add constraint chk_participants_status
        check (status in ('ACTIVE', 'INACTIVE'));

drop type user_status;

drop type participant_status;
