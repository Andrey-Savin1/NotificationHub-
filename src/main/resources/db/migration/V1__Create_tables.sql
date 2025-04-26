create table notifications
(
    id                        serial    primary key,
    created_at                timestamp default now() not null,
    modified_at               timestamp,
    message                   text                    not null,
    user_uid                  varchar(36),
    notification_status       varchar(32),
    subject                   varchar(128),
    created_by                varchar(255)
    );

