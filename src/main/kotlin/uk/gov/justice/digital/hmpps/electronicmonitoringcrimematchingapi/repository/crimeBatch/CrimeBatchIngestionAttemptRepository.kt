package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchIngestionAttempt
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection.CrimeBatchEmailAttachmentErrorProjection
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection.CrimeBatchIngestionAttemptProjection
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection.CrimeBatchIngestionAttemptSummaryProjection
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection.IngestionAttemptCrimesByTypeProjection
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

@Repository
interface CrimeBatchIngestionAttemptRepository : JpaRepository<CrimeBatchIngestionAttempt, UUID> {
  @Query(
    value = """
      SELECT
        cbia.id              AS ingestionAttemptId,
        cbia.created_at      AS createdAt,
        cb.id                AS crimeBatchId,
        cb.batch_id          AS batchId,
        cv.police_force_area AS policeForceArea,
        cmru.matches         AS matches,
        CASE
          WHEN cbea IS NULL THEN 'FAILED'
          WHEN COALESCE(cv.version_count, 0) = 0 AND cbea.row_count > 0 THEN 'ERROR'
          WHEN cv.version_count = cbea.row_count THEN 'SUCCESSFUL'
          WHEN COALESCE(cv.version_count, 0) < cbea.row_count THEN 'PARTIAL'
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
          cbia.id,
          cbia.created_at,
          cb.id AS crime_batch_id,
          cbea.file_name,
          cbea.row_count
        FROM crime_batch_ingestion_attempt cbia
        LEFT JOIN crime_batch_email cbe 
            ON cbia.id = cbe.crime_batch_ingestion_attempt_id
        LEFT JOIN crime_batch_email_attachment cbea 
            ON cbe.id = cbea.crime_batch_email_id
        LEFT JOIN crime_batch cb 
            ON cbea.id = cb.crime_batch_email_attachment_id
        WHERE cbia.id = :crimeBatchIngestionAttemptId
      ),

    crime_versions AS (
        SELECT
           cbcv.crime_batch_id,
           c.police_force_area,
           COUNT(*) AS version_count
        FROM crime_batch_crime_version cbcv
        JOIN ingestion_attempt ia 
            ON ia.crime_batch_id = cbcv.crime_batch_id
        LEFT JOIN crime_version cv 
            ON cbcv.crime_version_id = cv.id
        LEFT JOIN crime c 
            ON cv.crime_id = c.id
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
      JOIN ingestion_attempt ia 
        ON ia.crime_batch_id = r.crime_batch_id
    ),

    latest_run_with_counts AS (
      SELECT
        lr.crime_batch_id,
        COUNT(cmrdw.*) AS matches
      FROM latest_runs lr
      LEFT JOIN crime_matching_result cmre 
        ON cmre.crime_matching_run_id = lr.run_id
      LEFT JOIN crime_matching_result_device_wearer cmrdw 
        ON cmre.id = cmrdw.crime_matching_result_id
      WHERE lr.rn = 1
      GROUP BY lr.crime_batch_id
    )

    SELECT
      ia.id AS ingestionAttemptId,
      ia.created_at AS createdAt,
      ia.crime_batch_id AS batchId,
      cv.police_force_area AS policeForceArea,
      ia.file_name AS fileName,
      lrwc.matches AS matches,
      ia.row_count AS submitted,
      cv.version_count AS successful,
      ia.row_count - COALESCE(cv.version_count, 0) AS failed,
      CASE
        WHEN ia.file_name IS NULL THEN 'FAILED'
        WHEN COALESCE(cv.version_count, 0) = 0 AND ia.row_count > 0 THEN 'ERROR'
        WHEN cv.version_count = ia.row_count THEN 'SUCCESSFUL'
        WHEN COALESCE(cv.version_count, 0) < ia.row_count THEN 'PARTIAL'
        ELSE 'UNKNOWN'
      END AS ingestionStatus
    FROM ingestion_attempt ia
    LEFT JOIN crime_versions cv 
        ON cv.crime_batch_id = ia.crime_batch_id
    LEFT JOIN latest_run_with_counts lrwc 
        ON lrwc.crime_batch_id = ia.crime_batch_id;
    """,
    nativeQuery = true,
  )
  fun findCrimeBatchIngestionAttemptById(
    @Param("crimeBatchIngestionAttemptId") crimeBatchIngestionAttemptId: UUID,
  ): Optional<CrimeBatchIngestionAttemptProjection>

  @Query(
    value = """
        SELECT
         e.error_type AS errorType,
         e.field_name AS fieldName,
         e.value AS value,
         e.crime_reference AS crimeReference,
         e.row_number AS rowNumber,
         e.crime_type_id AS crimeType
        FROM crime_batch_ingestion_attempt cbia
        LEFT JOIN crime_batch_email cbe 
            ON cbia.id = cbe.crime_batch_ingestion_attempt_id
        LEFT JOIN crime_batch_email_attachment cbea 
            ON cbe.id = cbea.crime_batch_email_id
        JOIN crime_batch_email_attachment_ingestion_error e 
            ON cbea.id = e.crime_batch_email_attachment_id
        WHERE cbia.id = :crimeBatchIngestionAttemptId
    """,
    nativeQuery = true,
  )
  fun findIngestionAttemptValidationErrors(
    @Param("crimeBatchIngestionAttemptId") crimeBatchIngestionAttemptId: UUID,
  ): List<CrimeBatchEmailAttachmentErrorProjection>

  @Query(
    value = """
      WITH ingestion_attempt AS (
        SELECT
          cbia.id AS ingestion_attempt_id,
          cb.id   AS crime_batch_id,
          cbea.id AS attachment_id
        FROM crime_batch_ingestion_attempt cbia
        LEFT JOIN crime_batch_email cbe 
            ON cbia.id = cbe.crime_batch_ingestion_attempt_id
        LEFT JOIN crime_batch_email_attachment cbea 
            ON cbe.id = cbea.crime_batch_email_id
        LEFT JOIN crime_batch cb 
            ON cbea.id = cb.crime_batch_email_attachment_id
        WHERE cbia.id = :crimeBatchIngestionAttemptId
      ),
      
      attachment_ingestion_errors AS (
        SELECT
          ia.ingestion_attempt_id,
          ia.crime_batch_id,
          e.crime_type_id AS crimeType,
          COUNT(*)        AS failed
        FROM ingestion_attempt ia
        RIGHT JOIN crime_batch_email_attachment_ingestion_error e 
            ON ia.attachment_id = e.crime_batch_email_attachment_id
        GROUP BY ia.ingestion_attempt_id, ia.crime_batch_id, e.crime_type_id
      ),
      
      crime_versions AS (
        SELECT
          ia.ingestion_attempt_id,
          cbcv.crime_batch_id,
          cv.crime_type_id AS crimeType,
          COUNT(*) AS successful
        FROM ingestion_attempt ia
        JOIN crime_batch_crime_version cbcv 
            ON ia.crime_batch_id = cbcv.crime_batch_id
        LEFT JOIN crime_version cv 
            ON cbcv.crime_version_id = cv.id
        GROUP BY ia.ingestion_attempt_id, cbcv.crime_batch_id, cv.crime_type_id
      )
      
      SELECT
        COALESCE(a.crimeType, v.crimeType)                AS crimeType,
        COALESCE(a.failed, 0)                             AS failed,
        COALESCE(v.successful, 0)                         AS successful,
        COALESCE(a.failed, 0) + COALESCE(v.successful, 0) AS submitted
      FROM attachment_ingestion_errors a
      FULL JOIN crime_versions v 
        ON a.crime_batch_id = v.crime_batch_id
        AND a.crimeType = v.crimeType
    """,
    nativeQuery = true,
  )
  fun findIngestionAttemptCrimesByType(
    @Param("crimeBatchIngestionAttemptId") crimeBatchIngestionAttemptId: UUID,
  ): List<IngestionAttemptCrimesByTypeProjection>
}
