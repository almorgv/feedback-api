--liquibase formatted sql
--changeset mav:3
alter table "user"
    add column active boolean not null default true
--rollback alter table criteria drop column archived

