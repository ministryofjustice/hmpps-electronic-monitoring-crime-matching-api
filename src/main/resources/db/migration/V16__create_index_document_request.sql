ALTER TABLE document_request
ADD COLUMN claimed_at TIMESTAMPTZ;

-- For claiming eligible documents to generate
CREATE INDEX idx_document_request_type_status
    ON document_request (type, status);
