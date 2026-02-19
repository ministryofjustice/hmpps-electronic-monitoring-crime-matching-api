package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchIngestionAttempt
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.IngestionStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface CrimeBatchIngestionAttemptRepository : JpaRepository<CrimeBatchIngestionAttempt, UUID> {
  @Query(
    value = """
      SELECT
        cbia.id AS ingestionAttemptId,
        cbia.createdAt AS createdAt,
        cb.batchId AS batchId,
        c.policeForceArea AS policeForceArea,
        CASE 
          WHEN COUNT(cmru) = 0 THEN NULL
          ELSE COUNT(cmrdw)
        END AS matches,
        CASE 
          WHEN COUNT(cv) = 0 AND cbea.rowCount > 0 THEN 'FAILURE'
          WHEN cbea.rowCount = COUNT(cv) THEN 'SUCCESSFUL'
          ELSE 'PARTIAL'
        END AS status
      FROM CrimeBatchIngestionAttempt cbia
      JOIN cbia.crimeBatchEmail cbe
      JOIN cbe.crimeBatchEmailAttachments cbea
      JOIN cbea.crimeBatch cb
      JOIN cb.crimeVersions cv
      JOIN cv.crime c
      LEFT JOIN cb.crimeMatchingRuns cmru
      LEFT JOIN cmru.results cmre
      LEFT JOIN cmre.deviceWearers cmrdw
      WHERE cb.batchId LIKE CONCAT('%', COALESCE(:batchId, ''), '%')
      AND CAST(c.policeForceArea AS string) LIKE CONCAT('%', COALESCE(:policeForceArea, ''), '%')
      AND CAST(:fromDate AS date) IS NULL OR CAST(cbia.createdAt AS date) >= CAST(:fromDate AS date)
      AND CAST(:toDate AS date) IS NULL OR CAST(cbia.createdAt AS date) <= CAST(:toDate AS date)
      GROUP BY
        cbia.id,
        cbia.createdAt,
        cb.batchId,
        c.policeForceArea,
        cbea.rowCount
    """,
  )
  fun findByBatchId(
    @Param("batchId") batchId: String?,
    @Param("policeForceArea") policeForceArea: String?,
    @Param("fromDate") fromDate: LocalDateTime?,
    @Param("toDate") toDate: LocalDateTime?,
    pageable: Pageable,
  ): Page<IngestionAttemptSummary> // TODO map to Dto on call?
}

interface IngestionAttemptSummary {
  fun getIngestionAttemptId(): UUID
  fun getCreatedAt(): LocalDateTime
  fun getBatchId(): String?
  fun getPoliceForceArea(): PoliceForce?
  fun getMatches(): Long?
  fun getStatus(): IngestionStatus
}
