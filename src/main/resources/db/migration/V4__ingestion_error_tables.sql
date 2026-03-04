CREATE TABLE crime_batch_ingestion_error
(
    id                               UUID         NOT NULL,
    error_type                       VARCHAR(255) NOT NULL,
    crime_batch_ingestion_attempt_id UUID,
    CONSTRAINT pk_crime_batch_ingestion_error PRIMARY KEY (id)
);

ALTER TABLE crime_batch_email_attachment_ingestion_error
    ADD crime_type_id VARCHAR(255);

ALTER TABLE crime_batch_email_attachment_ingestion_error
    ADD field_name VARCHAR(255);

ALTER TABLE crime_batch_email_attachment_ingestion_error
    ADD value VARCHAR(255);

ALTER TABLE crime_batch_ingestion_error
    ADD CONSTRAINT uc_crime_batch_ingestion_error_crime_batch_ingestion_attempt UNIQUE (crime_batch_ingestion_attempt_id);

ALTER TABLE crime_batch_ingestion_error
    ADD CONSTRAINT FK_CRIME_BATCH_INGESTION_ERROR_ON_CRIME_BATCH_INGESTION_ATTEMPT FOREIGN KEY (crime_batch_ingestion_attempt_id) REFERENCES crime_batch_email (id);

ALTER TABLE crime_batch_email_attachment_ingestion_error
    ALTER COLUMN error_type SET NOT NULL;

ALTER TABLE crime_batch_email_attachment_ingestion_error
    ALTER COLUMN row_number TYPE BIGINT;