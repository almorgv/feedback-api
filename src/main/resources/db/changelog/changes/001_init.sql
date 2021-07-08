--liquibase formatted sql
--changeset mav:1
create table "user"
(
    id                 bigint not null,
    created_date       timestamp not null,
    last_modified_date timestamp not null,
    position           varchar not null,
    role               varchar not null,
    username           varchar not null,
    constraint user_pkey
        primary key (id),
    constraint uk_username
        unique (username)
);
create table review
(
    id                 bigint not null,
    created_date       timestamp not null,
    last_modified_date timestamp not null,
    period             varchar not null,
    user_id            bigint not null,
    constraint review_pkey
        primary key (id),
    constraint uk_user_id_period
        unique (user_id, period),
    constraint fk_user_id
        foreign key (user_id) references "user"
);
create table sheet
(
    id                 bigint not null,
    created_date       timestamp not null,
    last_modified_date timestamp not null,
    due_date           date not null,
    review_id          bigint not null,
    reviewer_id        bigint not null,
    constraint sheet_pkey
        primary key (id),
    constraint uk_review_id_reviewer_id
        unique (review_id, reviewer_id),
    constraint fk_review_id
        foreign key (review_id) references review,
    constraint fk_reviewer_id
        foreign key (reviewer_id) references "user"
);
create table criteria
(
    id                 bigint not null,
    created_date       timestamp not null,
    last_modified_date timestamp not null,
    description        varchar not null,
    name               varchar not null,
    role               varchar not null,
    constraint criteria_pkey
        primary key (id),
    constraint uk_name_role
        unique (name, role)
);
create table expectation
(
    id                 bigint not null,
    created_date       timestamp not null,
    last_modified_date timestamp not null,
    description        varchar not null,
    position           varchar not null,
    criteria_id        bigint,
    constraint expectation_pkey
        primary key (id),
    constraint uk_criteria_id_position
        unique (criteria_id, position),
    constraint fk_criteria_id
        foreign key (criteria_id) references criteria
);
create table answer
(
    id                 bigint not null,
    created_date       timestamp not null,
    last_modified_date timestamp not null,
    comment            varchar not null,
    score              integer not null,
    criteria_id        bigint not null,
    sheet_id           bigint not null,
    constraint answer_pkey
        primary key (id),
    constraint uk_sheet_id_criteria_id
        unique (sheet_id, criteria_id),
    constraint fk_criteria_id
        foreign key (criteria_id) references criteria,
    constraint fk_sheet_id
        foreign key (sheet_id) references sheet
);
create sequence hibernate_sequence;
--rollback drop table "user"
--rollback drop table review
--rollback drop table sheet
--rollback drop table criteria
--rollback drop table expectation
--rollback drop table answer
--rollback drop sequence hibernate_sequence
