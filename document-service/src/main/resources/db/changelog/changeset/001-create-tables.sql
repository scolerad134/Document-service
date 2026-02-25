create table document
(
    id bigserial primary key,
    unique_number varchar(50) unique,
    author varchar(255),
    title varchar(255),
    status varchar(20),
    created_at timestamp,
    updated_at timestamp
);

create table history
(
    id bigserial primary key,
    document_id bigint references document(id),
    action varchar(20),
    initiator varchar(255),
    comment text,
    created_at timestamp
);

create table approval_registry
(
    id bigserial primary key,
    document_id bigint unique references document(id),
    approved_by varchar(255),
    approved_at timestamp
);

create index idx_document_status on document (status);

create index idx_document_author on document (author);

create index idx_document_created_at on document (created_at);

create index idx_history_document_id on history (document_id);

create index idx_approval_registry_document_id on approval_registry (document_id);