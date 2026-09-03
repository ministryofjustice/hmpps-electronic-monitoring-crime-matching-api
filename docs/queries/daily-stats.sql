WITH ingestion_attempts AS (
    SELECT
        created_at::date AS date,
        COUNT(*) AS ingestion_attempts
    FROM crime_batch_ingestion_attempt
    GROUP BY created_at::date
),
crimes AS (
    SELECT
        created_at::date AS date,
        COUNT(*) AS crimes
    FROM crime_version
    GROUP BY created_at::date
),
matches AS (
    SELECT
        cmr.created_at::date AS date,
        COUNT(*) AS matches
    FROM crime_matching_result cmr
    INNER JOIN crime_matching_result_device_wearer cmrdw
        ON cmr.id = cmrdw.crime_matching_result_id
    GROUP BY cmr.created_at::date
),
sent_crimes AS (
    SELECT
        cbia.created_at::date AS date,
        COALESCE(SUM(cmbea.row_count), 0) AS sent_crimes
    FROM crime_batch_ingestion_attempt cbia
    LEFT JOIN crime_batch_email cmbe
        ON cbia.id = cmbe.crime_batch_ingestion_attempt_id
    LEFT JOIN crime_batch_email_attachment cmbea
        ON cmbe.id = cmbea.crime_batch_email_id
    GROUP BY cbia.created_at::date
)
SELECT
    COALESCE(
        ingestion_attempts.date,
        crimes.date,
        matches.date,
        sent_crimes.date
    ) AS date,
    COALESCE(ingestion_attempts.ingestion_attempts, 0) AS ingestion_attempts,
    COALESCE(sent_crimes.sent_crimes, 0) AS sent_crimes,
    COALESCE(crimes.crimes, 0) AS ingested_crimes,
    COALESCE(sent_crimes.sent_crimes, 0) - COALESCE(crimes.crimes, 0) AS not_ingested_crimes,
    COALESCE(matches.matches, 0) AS matches
FROM ingestion_attempts
FULL OUTER JOIN crimes
    ON crimes.date = ingestion_attempts.date
FULL OUTER JOIN matches
    ON matches.date = COALESCE(ingestion_attempts.date, crimes.date)
FULL OUTER JOIN sent_crimes
    ON sent_crimes.date = COALESCE(
        ingestion_attempts.date,
        crimes.date,
        matches.date
    )
ORDER BY date;
