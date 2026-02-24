package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeMatching

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeMatchingResult
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection.CrimeMatchingResultProjection
import java.util.UUID

@Repository
interface CrimeMatchingResultRepository : JpaRepository<CrimeMatchingResult, UUID> {
  @Query(
    value = """
      WITH latest_per_version AS (
        SELECT
          cb.batch_id                  AS batch_id,
          c.police_force_area          AS police_force_area,
          c.crime_reference            AS crime_reference,
          cv.crime_type_id             AS crime_type_id,
          cv.crime_date_time_from      AS crime_date_time_from,
          cv.crime_date_time_to        AS crime_date_time_to,
          cv.latitude                  AS crime_latitude,
          cv.longitude                 AS crime_longitude,
          cv.easting                   AS crime_easting,
          cv.northing                  AS crime_northing,
          cv.crime_text                AS crime_text,
          mr.id                        AS matching_result_id,
          ROW_NUMBER() OVER (
            PARTITION BY cb.id, cv.id
            ORDER BY run.matching_ended DESC, run.id DESC
          ) AS rn
        FROM crime_batch_crime_version bcv
        JOIN crime_batch cb ON cb.id = bcv.crime_batch_id
        JOIN crime_version cv ON cv.id = bcv.crime_version_id
        JOIN crime c ON c.id = cv.crime_id
        JOIN crime_matching_result mr ON mr.crime_version_id = cv.id
        JOIN crime_matching_run run ON run.id = mr.crime_matching_run_id
        WHERE cb.id = ANY(:batchIds)
          AND run.crime_batch_id = cb.id
      )
      SELECT
        lpv.police_force_area          AS policeForceArea,
        lpv.batch_id                   AS batchId,
        lpv.crime_reference            AS crimeReference,
        lpv.crime_type_id              AS crimeTypeId,
        lpv.crime_date_time_from       AS crimeDateTimeFrom,
        lpv.crime_date_time_to         AS crimeDateTimeTo,
        lpv.crime_latitude             AS crimeLatitude,
        lpv.crime_longitude            AS crimeLongitude,
        lpv.crime_easting              AS crimeEasting,
        lpv.crime_northing             AS crimeNorthing,
        lpv.crime_text                 AS crimeText,
        dw.device_id                   AS deviceId,
        dw.name                        AS name,
        dw.nomis_id                     AS nomisId
      FROM latest_per_version lpv
      JOIN crime_matching_result_device_wearer dw
        ON dw.crime_matching_result_id = lpv.matching_result_id
      WHERE lpv.rn = 1
      ORDER BY lpv.batch_id, lpv.crime_reference, dw.device_id
    """,
    nativeQuery = true,
  )
  fun findLatestCrimeMatchesByBatchIds(
    batchIds: Array<UUID>,
  ): List<CrimeMatchingResultProjection>
}
