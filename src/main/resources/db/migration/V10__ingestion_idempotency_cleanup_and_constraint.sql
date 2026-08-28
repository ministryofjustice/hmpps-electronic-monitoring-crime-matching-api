-- V10: Canonicalise pre-existing duplicate ingestion attempts and enforce source uniqueness.
-- This migration can run after V9 has been safely deployed.

CREATE TEMP TABLE tmp_crime_batch_ingestion_attempt_losers AS
WITH attempts_ranked AS (
    SELECT
        cbia.id AS ingestion_attempt_id,
        cbe.id AS crime_batch_email_id,
        cbea.id AS crime_batch_email_attachment_id,
        cb.id AS crime_batch_id,
        cbia.bucket,
        cbia.object_name,
        ROW_NUMBER() OVER (
            PARTITION BY cbia.bucket, cbia.object_name
            ORDER BY
                CASE WHEN cb.id IS NOT NULL THEN 0 ELSE 1 END,
                cbia.created_at,
                cbia.id
        ) AS row_rank
    FROM crime_batch_ingestion_attempt cbia
             LEFT JOIN crime_batch_email cbe
                       ON cbe.crime_batch_ingestion_attempt_id = cbia.id
             LEFT JOIN crime_batch_email_attachment cbea
                       ON cbea.crime_batch_email_id = cbe.id
             LEFT JOIN crime_batch cb
                       ON cb.crime_batch_email_attachment_id = cbea.id
    WHERE cbia.bucket IS NOT NULL
      AND cbia.object_name IS NOT NULL
),
duplicates AS (
    SELECT bucket, object_name
    FROM attempts_ranked
    GROUP BY bucket, object_name
    HAVING COUNT(*) > 1
)
SELECT
    keeper.ingestion_attempt_id AS kept_ingestion_attempt_id,
    loser.ingestion_attempt_id AS removed_ingestion_attempt_id,
    loser.crime_batch_email_id,
    loser.crime_batch_email_attachment_id,
    loser.crime_batch_id,
    loser.bucket,
    loser.object_name
FROM attempts_ranked loser
         JOIN attempts_ranked keeper
              ON loser.bucket = keeper.bucket
                  AND loser.object_name = keeper.object_name
                  AND keeper.row_rank = 1
         JOIN duplicates d
              ON d.bucket = loser.bucket
                  AND d.object_name = loser.object_name
WHERE loser.row_rank > 1;

INSERT INTO crime_batch_ingestion_attempt_dedup_audit (
    kept_id,
    removed_id,
    bucket,
    object_name,
    removed_at
)
SELECT
    kept_ingestion_attempt_id,
    removed_ingestion_attempt_id,
    bucket,
    object_name,
    NOW()
FROM tmp_crime_batch_ingestion_attempt_losers;

-- Delete in FK-safe order for non-cascading relationships.
DELETE FROM crime_batch_ingestion_error
WHERE crime_batch_ingestion_attempt_id IN (
    SELECT DISTINCT crime_batch_email_id
    FROM tmp_crime_batch_ingestion_attempt_losers
    WHERE crime_batch_email_id IS NOT NULL
);

DELETE FROM crime_batch_email_attachment_ingestion_error
WHERE crime_batch_email_attachment_id IN (
    SELECT DISTINCT crime_batch_email_attachment_id
    FROM tmp_crime_batch_ingestion_attempt_losers
    WHERE crime_batch_email_attachment_id IS NOT NULL
);

DELETE FROM crime_batch
WHERE id IN (
    SELECT DISTINCT crime_batch_id
    FROM tmp_crime_batch_ingestion_attempt_losers
    WHERE crime_batch_id IS NOT NULL
);

DELETE FROM crime_batch_email_attachment
WHERE id IN (
    SELECT DISTINCT crime_batch_email_attachment_id
    FROM tmp_crime_batch_ingestion_attempt_losers
    WHERE crime_batch_email_attachment_id IS NOT NULL
);

DELETE FROM crime_batch_email
WHERE id IN (
    SELECT DISTINCT crime_batch_email_id
    FROM tmp_crime_batch_ingestion_attempt_losers
    WHERE crime_batch_email_id IS NOT NULL
);

DELETE FROM crime_batch_ingestion_attempt
WHERE id IN (
    SELECT removed_ingestion_attempt_id
    FROM tmp_crime_batch_ingestion_attempt_losers
);

-- (bucket, object_name) is the idempotency key for email ingestion.
-- A unique constraint ensures at most one successful ingestion attempt per S3 object.
ALTER TABLE crime_batch_ingestion_attempt
    ADD CONSTRAINT uc_crime_batch_ingestion_attempt_source UNIQUE (bucket, object_name);

