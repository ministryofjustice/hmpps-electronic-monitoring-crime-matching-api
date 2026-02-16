ALTER TABLE crime_batch_email_attachment_ingestion_error
    ADD COLUMN crime_type_id VARCHAR(255);

ALTER TABLE crime_batch_email_attachment_ingestion_error
    ALTER COLUMN row_number TYPE BIGINT;
