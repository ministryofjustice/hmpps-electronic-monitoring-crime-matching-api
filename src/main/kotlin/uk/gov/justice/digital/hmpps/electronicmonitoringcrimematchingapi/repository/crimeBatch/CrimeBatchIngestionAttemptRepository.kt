package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchIngestionAttempt
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection.CrimeBatchIngestionAttemptProjection
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface CrimeBatchIngestionAttemptRepository : JpaRepository<CrimeBatchIngestionAttempt, UUID> {

  @Query(
    value = """
      SELECT
      CAST(cbia.id AS VARCHAR(36)) AS ingestionAttemptId, -- This casting is currently only used due to DB in tests treating UUID casting differently TODO
      cbia.created_at     AS createdAt,
      cb.batch_id         AS batchId,
      c.police_force_area AS policeForceArea,
      cmru.matches        AS matches,
      CASE
        WHEN cbea.row_count = 0 THEN 'SUCCESSFUL'
        WHEN cvcount.version_count = cbea.row_count THEN 'SUCCESSFUL'
        WHEN COALESCE(cvcount.version_count, 0) = 0 AND cbea.row_count > 0 THEN 'FAILURE'
        WHEN COALESCE(cvcount.version_count, 0) > 0 AND COALESCE(cvcount.version_count, 0) < cbea.row_count THEN 'PARTIAL'
        ELSE 'UNKNOWN'
      END AS ingestionStatus

      FROM crime_batch_ingestion_attempt cbia
      JOIN crime_batch_email cbe 
          ON cbia.id = cbe.crime_batch_ingestion_attempt_id
      LEFT JOIN crime_batch_email_attachment cbea 
          ON cbe.id = cbea.crime_batch_email_id
      LEFT JOIN crime_batch cb
          ON cbea.id = cb.crime_batch_email_attachment_id
      LEFT JOIN crime_batch_crime_version cbcv 
          ON cb.id = cbcv.crime_batch_id
      LEFT JOIN crime_version cv 
          ON cbcv.crime_version_id = cv.id
      LEFT JOIN crime c 
          ON cv.crime_id = c.id

      -- Get crime version count
      LEFT JOIN (
          SELECT crime_batch_id, COUNT(*) AS version_count
          FROM crime_batch_crime_version
          GROUP BY crime_batch_id
      ) cvcount ON cvcount.crime_batch_id = cb.id

      -- Get count of matches per batch by latest crime matching
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
        AND (:policeForceArea IS NULL OR LOWER(c.police_force_area) LIKE '%' || LOWER(:policeForceArea) || '%')
        AND (cbia.created_at >= COALESCE(:fromDate, cbia.created_at))
        AND (cbia.created_at <= COALESCE(:toDate, cbia.created_at))

      -- To handle only returning completed matches or failed ingestion attempts
      AND (cmru.matching_ended IS NOT NULL OR (COALESCE(cvcount.version_count, 0) = 0 AND cbea.row_count > 0))

  """,
    nativeQuery = true
  )
  fun findCrimeBatchIngestionAttempts(
    @Param("batchId") batchId: String?,
    @Param("policeForceArea") policeForceArea: String?,
    @Param("fromDate") fromDate: LocalDateTime?,
    @Param("toDate") toDate: LocalDateTime?,
    pageable: Pageable,
  ): Page<CrimeBatchIngestionAttemptProjection>
}
