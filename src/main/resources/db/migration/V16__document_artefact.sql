ALTER TABLE document_request
ADD COLUMN artefact_reference VARCHAR(500),
ADD COLUMN artefact_filename VARCHAR(255),
ADD COLUMN artefact_content_type VARCHAR(100);

