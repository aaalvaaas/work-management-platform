create table mail_messages
(
    id bigserial primary key,
    message_id varchar(255) not null unique,
    sender_email varchar(255) not null,
    subject varchar(255) not null,
    plain_text_body text not null,
    processing_status varchar(32) not null,
    received_at timestamp not null,
    processed_at timestamp
);
