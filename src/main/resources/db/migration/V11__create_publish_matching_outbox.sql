CREATE TABLE publish_matching_outbox
(
    id               UUID NOT NULL,
    crime_batch_ingestion_attempt_id UUID NOT NULL,
    state       VARCHAR(30) NOT NULL,
    created_at        TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_publish_matching_outbox PRIMARY KEY (id)
);

ALTER TABLE publish_matching_outbox
    ADD CONSTRAINT FK_PUBLISH_MATCHING_OUTBOX_ON_CRIME_BATCH_INGESTION_ATTEMPT
        FOREIGN KEY (crime_batch_ingestion_attempt_id)
            REFERENCES crime_batch_ingestion_attempt (id)
        ON DELETE CASCADE; -- Delete outbox row when the corresponding crime_batch_ingestion_attempt is deleted
