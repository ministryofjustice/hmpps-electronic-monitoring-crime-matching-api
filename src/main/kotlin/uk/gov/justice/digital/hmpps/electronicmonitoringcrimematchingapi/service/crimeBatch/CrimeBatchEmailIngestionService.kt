package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.EmailData
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmail
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmailAttachment
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmailAttachmentIngestionError
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchIngestionAttempt
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.MatchingPublishState
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.validation.EmailAttachmentIngestionError
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeBatchIngestionAttemptRepository
import java.util.UUID

@Service
class CrimeBatchEmailIngestionService(
  private val crimeBatchIngestionAttemptRepository: CrimeBatchIngestionAttemptRepository,
) {
  fun saveCrimeBatchIngestionAttempt(crimeBatchIngestionAttempt: CrimeBatchIngestionAttempt): CrimeBatchIngestionAttempt = crimeBatchIngestionAttemptRepository.save(crimeBatchIngestionAttempt)

  fun createCrimeBatchIngestionAttempt(bucketName: String, objectKey: String): CrimeBatchIngestionAttempt = CrimeBatchIngestionAttempt(
    bucket = bucketName,
    objectName = objectKey,
  )

  /**
   * Returns the ingestion attempt for the given S3 source coordinates, or null if none exists.
   * Used as the idempotency check before starting a new ingestion.
   */
  fun findIngestionAttemptBySource(bucket: String, objectName: String): CrimeBatchIngestionAttempt? = crimeBatchIngestionAttemptRepository.findByBucketAndObjectName(bucket, objectName).orElse(null)

  /**
   * Persists the confirmed [MatchingPublishState] for the given attempt.
   * Runs in its own transaction so a failure here does not roll back the upstream publish.
   */
  @Transactional
  fun markMatchingPublishState(attemptId: UUID, state: MatchingPublishState) {
    crimeBatchIngestionAttemptRepository.updateMatchingPublishState(attemptId, state)
  }

  fun createCrimeBatchEmail(emailData: EmailData, crimeBatchIngestionAttempt: CrimeBatchIngestionAttempt): CrimeBatchEmail = CrimeBatchEmail(
    sender = emailData.sender,
    originalSender = emailData.originalSender,
    subject = emailData.subject,
    sentAt = emailData.sentAt,
    crimeBatchIngestionAttempt = crimeBatchIngestionAttempt,
  )

  fun createCrimeBatchEmailAttachment(fileName: String, recordCount: Int, crimeBatchEmail: CrimeBatchEmail): CrimeBatchEmailAttachment = CrimeBatchEmailAttachment(
    fileName = fileName,
    rowCount = recordCount,
    crimeBatchEmail = crimeBatchEmail,
  )

  fun createCrimeBatchEmailAttachmentIngestionError(
    error: EmailAttachmentIngestionError,
    crimeBatchEmailAttachment: CrimeBatchEmailAttachment,
  ): CrimeBatchEmailAttachmentIngestionError = CrimeBatchEmailAttachmentIngestionError(
    rowNumber = error.rowNumber,
    crimeReference = error.crimeReference,
    crimeTypeId = error.crimeTypeId,
    errorType = error.errorType,
    fieldName = error.field,
    value = error.value,
    crimeBatchEmailAttachment = crimeBatchEmailAttachment,
  )
}
