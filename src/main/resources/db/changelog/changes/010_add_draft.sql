--liquibase formatted sql
--changeset ons:10
create table if not exists draft(
    id                      bigint not null,
    created_date            timestamp not null,
    last_modified_date      timestamp not null,
    user_id                 bigint not null,
    author_id               bigint not null,
    text                    varchar not null,
    constraint draft_pkey
        primary key (id),
    constraint uk_author_id_user_id
        unique (author_id, user_id),
    constraint fk_author_id
        foreign key (author_id) references "user",
    constraint fk_user_id
        foreign key (user_id) references "user"
);
--rollback drop table draft
