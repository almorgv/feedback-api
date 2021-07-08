--liquibase formatted sql
--changeset mav:6
alter table review
    add column user_position varchar(255)
--rollback alter table review drop column user_position;
