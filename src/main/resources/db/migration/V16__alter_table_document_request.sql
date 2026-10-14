ALTER TABLE document_request
ADD COLUMN claimed_at TIMESTAMPTZ,
ADD COLUMN attempts INT, -- TODO(ACR-1101): Not yet used
ADD COLUMN last_error TEXT, -- TODO(ACR-1101): Not yet used
ADD COLUMN concurrency_version BIGINT NOT NULL DEFAULT 0; -- TODO(ACR-1101): Not yet used

-- For claiming eligible documents to generate
CREATE INDEX idx_document_request_type_status
    ON document_request (type, status);
