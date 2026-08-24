-- (bucket, object_name) is the idempotency key for email ingestion.
-- A unique constraint ensures at most one successful ingestion attempt per S3 object,
-- so SQS redeliveries after a transient SNS failure cannot create duplicate batches.
ALTER TABLE crime_batch_ingestion_attempt
    ADD CONSTRAINT uc_crime_batch_ingestion_attempt_source UNIQUE (bucket, object_name);

-- Tracks whether the matching-notification SNS publish was confirmed.
--   UNKNOWN        – default; prior publish outcome not yet persisted (retry on redelivery).
--   PUBLISHED      – publish confirmed; duplicates safely skip.
--   NOT_APPLICABLE – FAILED/ERROR outcome; no publish needed.
ALTER TABLE crime_batch_ingestion_attempt
    ADD COLUMN matching_publish_state VARCHAR(255) NOT NULL DEFAULT 'UNKNOWN';

-- Denormalised crime_batch PK stored directly on the attempt so duplicate-delivery
-- handling can retry publishMatchingRequest without traversing the object graph.
ALTER TABLE crime_batch_ingestion_attempt
    ADD COLUMN crime_batch_id UUID;

