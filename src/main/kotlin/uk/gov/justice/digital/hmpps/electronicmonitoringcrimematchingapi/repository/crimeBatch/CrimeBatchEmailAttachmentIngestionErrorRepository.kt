package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmailAttachmentIngestionError
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection.FailedCrimeSubmissionProjection
import java.time.Instant
import java.util.UUID

@Repository
interface CrimeBatchEmailAttachmentIngestionErrorRepository : JpaRepository<CrimeBatchEmailAttachmentIngestionError, UUID> {

  @Query(
    value = """
      WITH ranked_errors AS (
        SELECT
          e.*,
          ROW_NUMBER() OVER (
            PARTITION BY e.crime_batch_email_attachment_id, e.row_number
            ORDER BY e.error_type
          ) AS error_rank
        FROM crime_batch_email_attachment_ingestion_error e
      )
      SELECT
        e.error_type AS reasonForFailure,
        CAST(COUNT(*) AS INTEGER) AS failureCount
      FROM ranked_errors e
      JOIN crime_batch_email_attachment attachment
        ON attachment.id = e.crime_batch_email_attachment_id
      JOIN crime_batch_email email
        ON email.id = attachment.crime_batch_email_id
      JOIN crime_batch_ingestion_attempt attempt
        ON attempt.id = email.crime_batch_ingestion_attempt_id
      WHERE e.error_rank = 1
        AND e.police_force_area = :policeForceArea
        AND (CAST(:from AS TIMESTAMP) IS NULL OR attempt.created_at >= :from)
        AND (CAST(:to AS TIMESTAMP) IS NULL OR attempt.created_at < :to)
      GROUP BY e.error_type
      ORDER BY e.error_type
    """,
    nativeQuery = true,
  )
  fun findFailedCrimeSubmissions(
    @Param("policeForceArea") policeForce: String,
    @Param("from") from: Instant?,
    @Param("to") to: Instant?,
  ): List<FailedCrimeSubmissionProjection>
}
