--liquibase formatted sql
--changeset mav:5
alter table "user"
    add column full_name   varchar(255) not null default '',
    add column email       varchar(255) not null default '',
    add column department  varchar(255) not null default '',
    add column appointment varchar(255) not null default '';
--rollback alter table "user" drop column fullName, drop column email, drop column department, drop column appointment;
