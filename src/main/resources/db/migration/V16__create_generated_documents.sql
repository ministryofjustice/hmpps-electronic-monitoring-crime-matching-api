ALTER TABLE document_request
    ADD COLUMN document BYTEA,
    ADD COLUMN document_content_type VARCHAR(50),
    ADD COLUMN concurrency_version INT NOT NULL;
-- TODO use concurrency_version to implement optimistic locking for document requests

-- So we can claim document requests efficiently for processing:
CREATE INDEX idx_document_request_type_status
    ON document_request (type, status);
