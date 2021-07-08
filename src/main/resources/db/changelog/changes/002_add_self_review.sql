--liquibase formatted sql
--changeset mav:2
create table self_review
(
    id                 bigint not null,
    created_date       timestamp not null,
    last_modified_date timestamp not null,
    description        varchar not null,
    good_things        varchar not null,
    bad_things         varchar not null,
    review_id          bigint not null,
    user_id            bigint not null,
    constraint self_review_pkey
        primary key (id),
    constraint uk_review_id_user_id
        unique (review_id, user_id),
    constraint fk_review_id
        foreign key (review_id) references review,
    constraint fk_user_id
        foreign key (user_id) references "user"
);
--rollback drop table self_review

