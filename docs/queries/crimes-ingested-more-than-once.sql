-- Identifies crimes that have been ingested more than once by finding crime records
-- with multiple versions for the same police force area and crime reference.
-- Results are ordered by the number of versions, highest first.

SELECT
    *
FROM (
    SELECT
        crime_reference,
        police_force_area,
        COUNT(*) as duplicates
    FROM crime
    LEFT JOIN crime_version
    ON crime.id = crime_version.crime_id
    GROUP BY crime_reference, police_force_area
)
WHERE duplicates > 1
ORDER BY duplicates DESC
