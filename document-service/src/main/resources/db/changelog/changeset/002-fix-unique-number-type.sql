-- liquibase formatted sql
-- changeset liquibase:002-fix-unique-number-type
ALTER TABLE document
    ALTER COLUMN unique_number TYPE uuid USING unique_number::uuid,
    ALTER COLUMN unique_number SET NOT NULL;
