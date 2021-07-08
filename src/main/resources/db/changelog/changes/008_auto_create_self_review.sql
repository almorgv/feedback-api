--liquibase formatted sql
--changeset mav:8
alter table self_review
    alter column description drop not null,
    alter column good_things drop not null,
    alter column bad_things drop not null
--rollback alter table self_review alter column description set not null, alter column good_things set not null, alter column bad_things set not null

