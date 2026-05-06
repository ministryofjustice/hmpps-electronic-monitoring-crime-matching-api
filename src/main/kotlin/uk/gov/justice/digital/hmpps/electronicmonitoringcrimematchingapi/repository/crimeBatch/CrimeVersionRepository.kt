package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeVersion
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection.CrimeVersionProjection
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection.CrimeVersionSummaryProjection
import java.util.UUID

@Repository
interface CrimeVersionRepository : JpaRepository<CrimeVersion, UUID> {
  @Query(
    value = """
      WITH base AS (
    SELECT
        vi.*,
        CASE
            WHEN EXISTS (
                SELECT 1
                FROM crime_version_update u
                WHERE u.crime_version_id = vi.id
            ) THEN 1 ELSE 0
            END AS has_updates

    FROM crime_version vi
),

     version_info AS (
         SELECT
             b.*,

             ROW_NUMBER() OVER (
                 PARTITION BY b.crime_id
                 ORDER BY b.created_at DESC
                 ) AS version_order,

             COUNT(*) OVER (
                 PARTITION BY b.crime_id
                 ) AS total_versions,

             FIRST_VALUE(b.id) OVER (
                 PARTITION BY b.crime_id
                 ORDER BY b.created_at DESC
                 ) AS latest_crime_version_id,

--          Increments the version number by 1 if there are updates
             SUM(CASE WHEN b.has_updates = 1 THEN 1 ELSE 0 END) OVER (
                 PARTITION BY b.crime_id
                 ORDER BY b.created_at
                 ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
                 ) + 1 AS version_number

         FROM base b
     )

SELECT
    cv.id AS crimeVersionId,
    c.crime_reference AS crimeReference,
    c.police_force_area AS policeForceArea,
    cv.crime_type_id AS crimeTypeId,
    cv.crime_date_time_from AS crimeDateTimeFrom,
    cb.batch_id AS batchId,
    cb.created_at AS ingestionDateTime,
    EXISTS (
        SELECT 1
        FROM crime_matching_result cmr
        WHERE cmr.crime_version_id = cv.id
    ) AS matched,
    CASE
        WHEN cv.id = vi.latest_crime_version_id THEN
            CASE
                WHEN vi.total_versions = 1 THEN 'Latest version'
                WHEN vi.has_updates = 0 THEN 'Latest version (Duplicate)'
                ELSE 'Latest version'
                END
        ELSE
            CASE
                -- first version
                WHEN vi.version_order = vi.total_versions AND vi.has_updates = 0 THEN
                    'Version ' || vi.version_number

                WHEN vi.has_updates = 0 THEN
                    'Version ' || vi.version_number || ' (Duplicate)'

                ELSE
                    'Version ' || vi.version_number
                END
        END AS versionLabel,
    'updates' AS updates
FROM version_info vi

         JOIN crime_version cv
              ON cv.id = vi.id

         JOIN crime_batch cb
              ON cb.id = cv.crime_batch_id

         JOIN crime c
              ON c.id = cv.crime_id

         LEFT JOIN crime_version_update cvu
                   ON cvu.crime_version_id = cv.id

         LEFT JOIN crime_matching_result cmr
                   ON cmr.id = (
                       SELECT id
                       FROM crime_matching_result
                       WHERE crime_version_id = cv.id
                       ORDER BY created_at DESC
                       LIMIT 1
                   )

         LEFT JOIN crime_matching_result_device_wearer cmrdw
                   ON cmrdw.crime_matching_result_id = cmr.id

         LEFT JOIN crime_matching_result_position cmrp
                   ON cmrp.crime_matching_result_device_wearer_id = cmrdw.id
WHERE LOWER(c.crime_reference) LIKE '%' || LOWER(:crimeReference) || '%'
ORDER BY cb.created_at DESC;

    """,

    countQuery = """
     SELECT COUNT(*)
     FROM crime_batch cb
     JOIN crime_version cv ON cv.crime_batch_id = cb.id
     JOIN crime c ON c.id = cv.crime_id
     WHERE (:crimeReference IS NULL
            OR LOWER(c.crime_reference) LIKE '%' || LOWER(:crimeReference) || '%')
    """,

    nativeQuery = true,
  )
  fun findCrimeVersionsByCrimeReference(
    crimeReference: String?,
    pageable: Pageable,
  ): Page<CrimeVersionSummaryProjection>

  @Query(
    value = """
    WITH base AS (
    SELECT
        vi.*,

        CASE
            WHEN EXISTS (
                SELECT 1
                FROM crime_version_update u
                WHERE u.crime_version_id = vi.id
            ) THEN 1 ELSE 0
        END AS has_updates

    FROM crime_version vi
),

version_info AS (
     SELECT
         b.*,
         ROW_NUMBER() OVER (
             PARTITION BY b.crime_id
             ORDER BY b.created_at DESC
         ) AS version_order,

         COUNT(*) OVER (
             PARTITION BY b.crime_id
         ) AS total_versions,

         FIRST_VALUE(b.id) OVER (
             PARTITION BY b.crime_id
             ORDER BY b.created_at DESC
         ) AS latest_crime_version_id,

         SUM(CASE WHEN b.has_updates = 1 THEN 1 ELSE 0 END) OVER (
             PARTITION BY b.crime_id
             ORDER BY b.created_at
             ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
         ) + 1 AS version_number

     FROM base b
)

SELECT
    cv.id AS crimeVersionId,

    c.crime_reference AS crimeReference,
    cb.batch_id AS batchId,

    cv.crime_type_id AS crimeType,
    cv.crime_date_time_from AS crimeDateTimeFrom,
    cv.crime_date_time_to AS crimeDateTimeTo,
    cv.crime_text AS crimeText,

    cv.latitude AS crimeLatitude,
    cv.longitude AS crimeLongitude,
    cv.northing AS crimeNorthing,
    cv.easting AS crimeEasting,

    cmr.id AS matchingResultId,

    cmrdw.id AS deviceWearerId,
    cmrdw.name AS name,
    cmrdw.device_id AS deviceId,
    cmrdw.nomis_id AS nomisId,

    cmrp.latitude AS wearerLatitude,
    cmrp.longitude AS wearerLongitude,
    cmrp.sequence_label AS sequenceLabel,
    cmrp.confidence_circle AS confidence,
    cmrp.captured_date_time AS capturedDateTime,

    CASE
        WHEN cv.id = vi.latest_crime_version_id THEN
            CASE
                WHEN vi.total_versions = 1 THEN 'Latest version'
                WHEN vi.has_updates = 0 THEN 'Latest version (Duplicate)'
                ELSE 'Latest version'
                END
        ELSE
            CASE
                WHEN vi.version_order = vi.total_versions AND vi.has_updates = 0 THEN
                    'Version ' || vi.version_number

                WHEN vi.has_updates = 0 THEN
                    'Version ' || vi.version_number || ' (Duplicate)'

                ELSE
                    'Version ' || vi.version_number
                END
        END AS versionLabel,
    CASE
        WHEN cv.id = vi.latest_crime_version_id THEN NULL
        ELSE vi.latest_crime_version_id
    END AS latestCrimeVersionId

FROM version_info vi

JOIN crime_version cv
  ON cv.id = vi.id

JOIN crime_batch cb
  ON cb.id = cv.crime_batch_id

JOIN crime c
  ON c.id = cv.crime_id

LEFT JOIN crime_version_update cvu
       ON cvu.crime_version_id = cv.id

LEFT JOIN crime_matching_result cmr
       ON cmr.id = (
           SELECT id
           FROM crime_matching_result
           WHERE crime_version_id = cv.id
           ORDER BY created_at DESC
           LIMIT 1
       )

LEFT JOIN crime_matching_result_device_wearer cmrdw
       ON cmrdw.crime_matching_result_id = cmr.id

LEFT JOIN crime_matching_result_position cmrp
       ON cmrp.crime_matching_result_device_wearer_id = cmrdw.id
WHERE cv.id =:crimeVersionId
ORDER BY cmrp.captured_date_time;

    """,
    nativeQuery = true,
  )
  fun findCrimeVersionMatchingResult(crimeVersionId: UUID): List<CrimeVersionProjection>

  fun findFirstByCrimeIdOrderByCreatedAtDesc(crimeId: UUID): CrimeVersion?
}
