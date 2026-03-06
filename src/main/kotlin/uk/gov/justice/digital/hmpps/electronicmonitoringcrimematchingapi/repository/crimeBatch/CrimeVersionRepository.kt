package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeVersion
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection.CrimeVersionSummaryProjection
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

@Repository
interface CrimeVersionRepository : JpaRepository<CrimeVersion, UUID> {
  fun findByCrimeIdAndCrimeTypeIdAndCrimeDateTimeFromAndCrimeDateTimeToAndEastingAndNorthingAndLatitudeAndLongitudeAndCrimeText(
    crimeId: UUID,
    crimeTypeId: CrimeType,
    crimeDateTimeFrom: LocalDateTime,
    crimeDateTimeTo: LocalDateTime,
    easting: Double?,
    northing: Double?,
    latitude: Double?,
    longitude: Double?,
    crimeText: String,
  ): Optional<CrimeVersion>

  @Query(
    value = """
      WITH versioned_crimes AS (
        SELECT
          cv.id                        AS crimeVersionId,
          c.crime_reference            AS crimeReference,
          c.police_force_area          AS policeForceArea,
          cv.crime_type_id             AS crimeTypeId,
          cv.crime_date_time_from      AS crimeDateTimeFrom,
          cv.crime_date_time_to        AS crimeDateTimeTo,
          cv.easting                   AS easting,
          cv.northing                  AS northing,
          cv.latitude                  AS latitude,
          cv.longitude                 AS longitude,
          cb.batch_id                  AS batchId,
          cb.created_at                AS ingestionDateTime,

          ROW_NUMBER() OVER (
            PARTITION BY c.id
            ORDER BY cb.created_at DESC, cv.id DESC
          ) AS version_number,

          -- Retrieve previous version values for the same crime.
          -- These are compared against the current row to determine what fields changed.
          LAG(cv.crime_type_id) OVER (
            PARTITION BY c.id
            ORDER BY cb.created_at, cv.id
          ) AS prev_crime_type,

          LAG(cv.crime_date_time_from) OVER (
            PARTITION BY c.id
            ORDER BY cb.created_at, cv.id
          ) AS prev_crime_date_from,

          LAG(cv.crime_date_time_to) OVER (
            PARTITION BY c.id
            ORDER BY cb.created_at, cv.id
          ) AS prev_crime_date_to,

          LAG(cv.easting) OVER (
            PARTITION BY c.id
            ORDER BY cb.created_at, cv.id
          ) AS prev_easting,

          LAG(cv.northing) OVER (
            PARTITION BY c.id
            ORDER BY cb.created_at, cv.id
          ) AS prev_northing,

          LAG(cv.latitude) OVER (
            PARTITION BY c.id
            ORDER BY cb.created_at, cv.id
          ) AS prev_latitude,

          LAG(cv.longitude) OVER (
            PARTITION BY c.id
            ORDER BY cb.created_at, cv.id
          ) AS prev_longitude

        FROM crime_batch_crime_version cbcv
        JOIN crime_batch cb ON cb.id = cbcv.crime_batch_id
        JOIN crime_version cv ON cv.id = cbcv.crime_version_id
        JOIN crime c ON c.id = cv.crime_id
        WHERE (:crimeReference IS NULL OR LOWER(c.crime_reference) LIKE LOWER(:crimeReference) || '%')
      )

      SELECT
        crimeVersionId,
        crimeReference,
        policeForceArea,
        crimeTypeId,
        crimeDateTimeFrom,
        batchId,
        ingestionDateTime,

        EXISTS (
          SELECT 1
          FROM crime_matching_result cmr
          WHERE cmr.crime_version_id = crimeVersionId
        ) AS matched,

        CASE
          WHEN version_number = 1 THEN 'Latest version'
          ELSE 'Version ' || version_number
        END AS versionLabel,

        CASE
          WHEN version_number = 1 THEN 'NA'
          ELSE concat_ws(', ',
            CASE WHEN prev_crime_type IS DISTINCT FROM crimeTypeId THEN 'Crime type' END,
            CASE WHEN prev_crime_date_from IS DISTINCT FROM crimeDateTimeFrom THEN 'Crime date' END,
            CASE WHEN prev_crime_date_to IS DISTINCT FROM crimeDateTimeTo THEN 'Crime time' END,
            CASE
              WHEN prev_easting IS DISTINCT FROM easting
                OR prev_northing IS DISTINCT FROM northing
                OR prev_latitude IS DISTINCT FROM latitude
                OR prev_longitude IS DISTINCT FROM longitude
              THEN 'Crime location'
            END
          )
        END AS updates

      FROM versioned_crimes
      ORDER BY ingestionDateTime DESC, crimeVersionId DESC
    """,
    countQuery = """
      SELECT COUNT(*)
      FROM crime_batch_crime_version cbcv
      JOIN crime_batch cb ON cb.id = cbcv.crime_batch_id
      JOIN crime_version cv ON cv.id = cbcv.crime_version_id
      JOIN crime c ON c.id = cv.crime_id
      WHERE (:crimeReference IS NULL OR LOWER(c.crime_reference) LIKE LOWER(:crimeReference) || '%')
    """,
    nativeQuery = true,
  )
  fun findCrimeVersionsByCrimeReference(
    crimeReference: String?,
    pageable: Pageable,
  ): Page<CrimeVersionSummaryProjection>
}
