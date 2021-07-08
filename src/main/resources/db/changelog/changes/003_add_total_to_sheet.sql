--liquibase formatted sql
--changeset mav:3
alter table sheet
    add column total_score integer,
    add column comment varchar
--rollback alter table sheet drop column total_score, drop column comment

