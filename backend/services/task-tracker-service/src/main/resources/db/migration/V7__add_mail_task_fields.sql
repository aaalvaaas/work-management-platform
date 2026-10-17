alter table tasks
    alter column author_user_id drop not null;

alter table tasks
    add column requester_email varchar(320);

create unique index uq_tasks_mail_source_reference
    on tasks (source_reference)
    where source = 'MAIL';
