DROP TABLE crime_batch_crime_version;

ALTER TABLE crime_version
    ADD COLUMN crime_batch_id UUID;

ALTER TABLE crime_version
    ADD CONSTRAINT FK_CRIME_VERSION_ON_CRIME_BATCH FOREIGN KEY (crime_batch_id) REFERENCES crime_batch (id)
        ON DELETE CASCADE;