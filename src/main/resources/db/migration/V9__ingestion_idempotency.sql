-- V9: Introduce idempotency infrastructure without enforcing uniqueness yet.
-- Pre-existing duplicates created by the former non-idempotent listener are handled in V10.

-- Audit table populated by V10 cleanup to record canonicalisation decisions.
CREATE TABLE crime_batch_ingestion_attempt_dedup_audit
(
    kept_id     UUID         NOT NULL,
    removed_id  UUID         NOT NULL,
    bucket      VARCHAR(255) NOT NULL,
    object_name VARCHAR(255) NOT NULL,
    removed_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_crime_batch_ingestion_attempt_dedup_audit PRIMARY KEY (removed_id)
);

-- Tracks whether the matching-notification SNS publish was confirmed.
--   PENDING_OR_UNCONFIRMED - default; publish is pending or the prior publish outcome was
--                            not yet persisted (retry on redelivery).
--   PUBLISHED   - publish confirmed; duplicates safely skip.
--   NOT_REQUIRED - FAILED/ERROR outcome; no publish needed.
ALTER TABLE crime_batch_ingestion_attempt
    ADD COLUMN matching_publish_state VARCHAR(255) NOT NULL DEFAULT 'PENDING_OR_UNCONFIRMED';

-- Denormalised crime_batch PK stored directly on the attempt so duplicate-delivery
-- handling can retry publishMatchingRequest without traversing the object graph.
ALTER TABLE crime_batch_ingestion_attempt
    ADD COLUMN crime_batch_id UUID;
