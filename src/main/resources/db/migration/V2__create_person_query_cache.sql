DROP TABLE IF EXISTS subject_query_cache;

CREATE TABLE persons_query_cache
(
    id                 UUID NOT NULL,
    nomis_id           VARCHAR(255),
    person_name        VARCHAR(255),
    device_id          VARCHAR(255),
    query_execution_id VARCHAR(255) NOT NULL,
    query_owner        VARCHAR(255) NOT NULL,
    created_at         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_persons_query_cache PRIMARY KEY (id)
);