package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchIngestionAttempt
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection.CrimeBatchIngestionAttemptProjection
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection.CrimeBatchIngestionAttemptSummaryProjection
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

@Repository
interface CrimeBatchIngestionAttemptRepository : JpaRepository<CrimeBatchIngestionAttempt, UUID> {

  @Query(
    value = """
      SELECT
        cbia.id AS ingestionAttemptId,
        cbia.created_at     AS createdAt,
        cb.batch_id         AS batchId,
        cv.police_force_area AS policeForceArea,
        cmru.matches        AS matches,
        CASE
          WHEN cbea.row_count = 0 THEN 'SUCCESSFUL'
          WHEN cv.version_count = cbea.row_count THEN 'SUCCESSFUL'
          WHEN COALESCE(cv.version_count, 0) = 0 AND cbea.row_count > 0 THEN 'FAILED'
          WHEN COALESCE(cv.version_count, 0) > 0 AND COALESCE(cv.version_count, 0) < cbea.row_count THEN 'PARTIAL'
          ELSE 'UNKNOWN'
        END AS ingestionStatus

      FROM crime_batch_ingestion_attempt cbia
      JOIN crime_batch_email cbe 
          ON cbia.id = cbe.crime_batch_ingestion_attempt_id
      LEFT JOIN crime_batch_email_attachment cbea 
          ON cbe.id = cbea.crime_batch_email_id
      LEFT JOIN crime_batch cb
          ON cbea.id = cb.crime_batch_email_attachment_id

      -- Get crime version details
      LEFT JOIN (
          SELECT crime_batch_id,
            c.police_force_area,
            COUNT(*) AS version_count
          FROM crime_batch_crime_version cbcv
          LEFT JOIN crime_version cv
            ON cbcv.crime_version_id = cv.id
          LEFT JOIN crime c
            ON cv.crime_id = c.id
          GROUP BY crime_batch_id,
            c.police_force_area
      ) cv ON cv.crime_batch_id = cb.id

      -- Get count of matched device_wearers per batch by latest crime matching
      LEFT JOIN (
          SELECT r.id,
                 r.crime_batch_id,
                 r.matching_ended,
                 (
                     SELECT COUNT(*)
                     FROM crime_matching_result cmre
                     JOIN crime_matching_result_device_wearer cmrdw
                        ON cmre.id = cmrdw.crime_matching_result_id
                     WHERE cmre.crime_matching_run_id = r.id
                 ) AS matches
          FROM crime_matching_run r
          JOIN (
              SELECT crime_batch_id, MAX(matching_ended) AS max_end
              FROM crime_matching_run
              GROUP BY crime_batch_id
          ) latest
            ON latest.crime_batch_id = r.crime_batch_id
           AND latest.max_end = r.matching_ended
      ) cmru ON cmru.crime_batch_id = cb.id

      -- Filtering
      WHERE (:batchId IS NULL OR LOWER(cb.batch_id) LIKE '%' || LOWER(:batchId) || '%')
        AND (:policeForceArea IS NULL OR LOWER(cv.police_force_area) LIKE '%' || LOWER(:policeForceArea) || '%')
        AND (cbia.created_at >= COALESCE(:fromDate, cbia.created_at))
        AND (cbia.created_at <= COALESCE(:toDate, cbia.created_at))

      -- To handle only returning completed matches or failed ingestion attempts
      AND (cmru.matching_ended IS NOT NULL OR (COALESCE(cv.version_count, 0) = 0 AND cbea.row_count > 0))

  """,
    nativeQuery = true,
  )
  fun findCrimeBatchIngestionAttempts(
    @Param("batchId") batchId: String?,
    @Param("policeForceArea") policeForceArea: String?,
    @Param("fromDate") fromDate: LocalDateTime?,
    @Param("toDate") toDate: LocalDateTime?,
    pageable: Pageable,
  ): Page<CrimeBatchIngestionAttemptSummaryProjection>

  @Query(
    value = """
      WITH ingestion_attempt AS (
        SELECT
          cbia.id AS ingestionAttemptId,
          cb.id AS crime_batch_id
        FROM crime_batch_ingestion_attempt cbia
        LEFT JOIN crime_batch_email cbe ON cbia.id = cbe.crime_batch_ingestion_attempt_id
        LEFT JOIN crime_batch_email_attachment cbea ON cbe.id = cbea.crime_batch_email_id
        LEFT JOIN crime_batch cb ON cbea.id = cb.crime_batch_email_attachment_id
        WHERE cbia.id = :crimeBatchIngestionAttemptId
      ),

      crime_versions AS (
        SELECT
          cbcv.crime_batch_id,
          c.police_force_area,
          COUNT(*) AS version_count
        FROM crime_batch_crime_version cbcv
        LEFT JOIN crime_version cv ON cbcv.crime_version_id = cv.id
        LEFT JOIN crime c ON cv.crime_id = c.id
        JOIN ingestion_attempt ia ON ia.crime_batch_id = cbcv.crime_batch_id
        GROUP BY cbcv.crime_batch_id, c.police_force_area
      ),

      latest_runs AS (
        SELECT
          r.id AS run_id,
          r.crime_batch_id,
          ROW_NUMBER() OVER (
            PARTITION BY r.crime_batch_id
            ORDER BY r.matching_ended DESC, r.id DESC
          ) AS rn
        FROM crime_matching_run r
        JOIN ingestion_attempt ia ON ia.crime_batch_id = r.crime_batch_id
      ),

      latest_run_with_counts AS (
        SELECT
         lr.crime_batch_id,
         COUNT(cmrdw.*) AS matches
        FROM latest_runs lr
        LEFT JOIN crime_matching_result cmre ON cmre.crime_matching_run_id = lr.run_id
        LEFT JOIN crime_matching_result_device_wearer cmrdw ON cmre.id = cmrdw.crime_matching_result_id
        WHERE lr.rn = 1
        GROUP BY lr.crime_batch_id
      )

      SELECT
        cbia.id AS ingestionAttemptId,
        cbia.created_at AS createdAt,
        cb.batch_id AS batchId,
        cv.police_force_area AS policeForceArea,
        cbea.file_name AS fileName,
        lrwc.matches AS matches,
        CASE
          WHEN cbea.row_count = 0 THEN 'SUCCESSFUL'
          WHEN cv.version_count = cbea.row_count THEN 'SUCCESSFUL'
          WHEN COALESCE(cv.version_count, 0) = 0 AND cbea.row_count > 0 THEN 'FAILED'
          WHEN COALESCE(cv.version_count, 0) < cbea.row_count THEN 'PARTIAL'
          ELSE 'UNKNOWN'
        END AS ingestionStatus

      FROM crime_batch_ingestion_attempt cbia
      JOIN crime_batch_email cbe ON cbia.id = cbe.crime_batch_ingestion_attempt_id
      LEFT JOIN crime_batch_email_attachment cbea ON cbe.id = cbea.crime_batch_email_id
      LEFT JOIN crime_batch cb ON cbea.id = cb.crime_batch_email_attachment_id
      LEFT JOIN crime_versions cv ON cv.crime_batch_id = cb.id
      LEFT JOIN latest_run_with_counts lrwc ON lrwc.crime_batch_id = cb.id
    """,
    nativeQuery = true,
  )
  fun findCrimeBatchIngestionAttemptById(
    @Param("crimeBatchIngestionAttemptId") crimeBatchIngestionAttemptId: UUID,
  ): Optional<CrimeBatchIngestionAttemptProjection>
}
