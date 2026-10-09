CREATE TABLE document_request
(
    id          UUID PRIMARY KEY,
    type        VARCHAR(50) NOT NULL,
    status      VARCHAR(50) NOT NULL,
    parameters  JSONB NOT NULL,
    created_by  VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL
);