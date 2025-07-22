CREATE TABLE subject_query_cache
(
    id                 BIGSERIAL NOT NULL,
    nomis_id           VARCHAR(255),
    subject_name       VARCHAR(255),
    query_execution_id VARCHAR(255) NOT NULL,
    query_owner        VARCHAR(255) NOT NULL,
    created_at         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_subject_query_cache PRIMARY KEY (id)
);