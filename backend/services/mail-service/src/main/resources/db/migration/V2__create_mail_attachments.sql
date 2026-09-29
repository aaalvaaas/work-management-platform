create table mail_attachments
(
    id bigserial primary key,
    mail_message_id bigint not null,
    filename varchar(255) not null,
    content_type varchar(255) not null,
    size bigint not null,
    storage_key varchar(255) not null,

    constraint fk_mail_attachments_mail_message foreign key (mail_message_id) references mail_messages(id) on delete cascade,

    constraint uk_mail_attachments_storage_key unique (storage_key)
)
