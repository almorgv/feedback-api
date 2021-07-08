--liquibase formatted sql
--changeset mav:8
alter table "user"
    add column if not exists active boolean not null default true
--rollback alter table criteria drop column archived

