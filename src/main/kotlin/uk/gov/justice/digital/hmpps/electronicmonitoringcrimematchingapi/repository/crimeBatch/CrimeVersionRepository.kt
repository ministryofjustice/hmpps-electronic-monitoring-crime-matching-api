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
      WITH version_info AS (
        SELECT
          cv.*,
          COUNT(*) OVER (PARTITION BY cv.crime_id) AS version_count,
          ROW_NUMBER() OVER (
            PARTITION BY cv.crime_id
            ORDER BY cv.created_at DESC
          ) AS latest_version,
          SUM(
            CASE
              WHEN cv.updates IS NOT NULL AND cv.updates <> '' THEN 1
              ELSE 0
            END
          ) OVER (
            PARTITION BY cv.crime_id
            ORDER BY cv.created_at
          ) + 1 AS version_number
        FROM crime_version cv
      )
      
      SELECT
        vi.id AS crimeVersionId,
        c.crime_reference AS crimeReference,
        c.police_force_area AS policeForceArea,
        vi.crime_type_id AS crimeTypeId,
        vi.crime_date_time_from AS crimeDateTimeFrom,
        cb.batch_id AS batchId,
        cb.created_at AS ingestionDateTime,
        EXISTS (
            SELECT 1
            FROM crime_matching_result cmr
            WHERE cmr.crime_version_id = vi.id
        ) AS matched,
        CASE
          WHEN vi.latest_version = 1 THEN
            CASE
              WHEN vi.updates = '' THEN 'Latest version (Duplicate)'
              ELSE 'Latest version'
            END
          ELSE
            'Version ' || vi.version_number ||
            CASE
              WHEN vi.updates = '' THEN ' (Duplicate)'
              ELSE ''
            END
        END AS versionLabel,
        CASE
          WHEN vi.updates = '' THEN 'None'
          WHEN vi.updates IS NULL THEN 'N/A'
          ELSE vi.updates
        END AS updates
      FROM version_info vi
      JOIN crime_batch cb 
          ON cb.id = vi.crime_batch_id
      JOIN crime c 
          ON c.id = vi.crime_id
      LEFT JOIN crime_matching_result cmr 
          ON cmr.id = (
            SELECT id
            FROM crime_matching_result
            WHERE crime_version_id = vi.id
            ORDER BY created_at DESC
            LIMIT 1
          )
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
    WITH version_info AS (
      SELECT
        cv.*,
        COUNT(*) OVER (PARTITION BY cv.crime_id) AS version_count,
        ROW_NUMBER() OVER (
          PARTITION BY cv.crime_id
          ORDER BY cv.created_at DESC
        ) AS latest_version,
        FIRST_VALUE(cv.id) OVER (
          PARTITION BY cv.crime_id
          ORDER BY cv.created_at DESC
        ) AS latest_crime_version_id,
        SUM(
          CASE
            WHEN cv.updates IS NOT NULL AND cv.updates <> '' THEN 1
            ELSE 0
          END
        ) OVER (
          PARTITION BY cv.crime_id
          ORDER BY cv.created_at
        ) + 1 AS version_number
      FROM crime_version cv
    )

    SELECT
      vi.id AS crimeVersionId,
      CASE
        WHEN vi.latest_version = 1 THEN NULL
        ELSE vi.latest_crime_version_id
      END AS latestCrimeVersionId,
      c.crime_reference AS crimeReference,
      cb.batch_id AS batchId,
      vi.crime_type_id AS crimeType,
      vi.crime_date_time_from AS crimeDateTimeFrom,
      vi.crime_date_time_to AS crimeDateTimeTo,
      vi.crime_text AS crimeText,
      vi.latitude AS crimeLatitude,
      vi.longitude AS crimeLongitude,
      vi.northing AS crimeNorthing,
      vi.easting AS crimeEasting,
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
        WHEN vi.latest_version = 1 THEN
          CASE
            WHEN vi.updates = '' THEN 'Latest version (Duplicate)'
            ELSE 'Latest version'
          END
        ELSE
          'Version ' || vi.version_number ||
          CASE
            WHEN vi.updates = '' THEN ' (Duplicate)'
            ELSE ''
          END
      END AS versionLabel
    FROM version_info vi
    JOIN crime_batch cb ON cb.id = vi.crime_batch_id
    JOIN crime c ON c.id = vi.crime_id
    LEFT JOIN crime_matching_result cmr ON cmr.id = (
      SELECT id
      FROM crime_matching_result
      WHERE crime_version_id = vi.id
      ORDER BY created_at DESC
      LIMIT 1
    )
    LEFT JOIN crime_matching_result_device_wearer cmrdw
        ON cmrdw.crime_matching_result_id = cmr.id
    LEFT JOIN crime_matching_result_position cmrp
        ON cmrp.crime_matching_result_device_wearer_id = cmrdw.id
    WHERE vi.id =:crimeVersionId
    ORDER BY cmrp.captured_date_time
    """,
    nativeQuery = true,
  )
  fun findCrimeVersionMatchingResult(crimeVersionId: UUID): List<CrimeVersionProjection>

  fun findFirstByCrimeIdOrderByCreatedAtDesc(crimeId: UUID): CrimeVersion?
}
