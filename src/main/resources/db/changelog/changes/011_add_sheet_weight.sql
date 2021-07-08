--liquibase formatted sql
--changeset ons:11
alter table "sheet"
    add column if not exists weight double precision default null
--rollback alter table sheet drop column weight
