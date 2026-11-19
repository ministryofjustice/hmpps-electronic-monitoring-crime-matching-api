ALTER TABLE crime_batch_email_attachment_ingestion_error
    ADD COLUMN police_force_area VARCHAR(255) NOT NULL DEFAULT 'UNKNOWN';