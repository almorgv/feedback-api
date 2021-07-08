--liquibase formatted sql
--changeset mav:4
create table job_role
(
    id                 bigint not null,
    created_date       timestamp,
    last_modified_date timestamp,
    name               varchar(255),
    primary key (id)
);
alter table "user"
    drop column role,
    add column user_role   varchar(50),
    add column job_role_id bigint,
    add constraint fk_job_role_id foreign key (job_role_id) references job_role;
alter table criteria
    drop column role,
    add column job_role_id bigint,
    add constraint fk_job_role_id foreign key (job_role_id) references job_role,
    add constraint uk_job_role_id unique (name, job_role_id);
create table sheet_answer
(
    id                 bigint not null,
    created_date       timestamp,
    last_modified_date timestamp,
    sheet_id           bigint,
    total_score        integer,
    comment            varchar,
    primary key (id),
    constraint uk_sheet_id unique (sheet_id)
);
alter table sheet
    drop column total_score,
    drop column comment,
    add column reviewer_group  varchar not null default 'COLLEGUE',
    add column completed       boolean not null default false,
    add column completed_date  timestamp,
    add column sheet_answer_id bigint,
    add constraint fk_sheet_answer_id foreign key (sheet_answer_id) references sheet_answer;
alter table answer
    alter column comment drop not null,
    alter column score drop not null;
alter table review
    add column completed      boolean not null default false,
    add column completed_date timestamp;
alter table self_review
    drop column user_id,
    add constraint uk_review_id unique (review_id)
--rollback drop table job_role
--rollback alter table "user" drop column user_role, drop column job_role_id, drop constraint fk_job_role_id, add column role varchar not null
--rollback alter table criteria drop column job_role_id, drop constraint fk_job_role_id, drop constraint uk_job_role_id, add column role varchar not null
--rollback drop table sheet_answer
--rollback alter table sheet add column total_score integer, add column comment varchar, drop column reviewer_group, drop column completed, drop column completed_date, drop column sheet_answer_id, drop constraint fk_sheet_answer_id
--rollback alter table answer alter column comment set not null, alter column score set not null
--rollback alter table review drop column completed, drop column completed_date
--rollback alter table self_review add column user_id bigint, add constraint uk_review_id_user_id unique (review_id, user_id), add constraint fk_user_id foreign key (user_id) references "user"

