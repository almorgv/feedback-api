--liquibase formatted sql
--changeset mav:7
alter table criteria
    add column if not exists archived boolean not null default false
--rollback alter table criteria drop column archived

