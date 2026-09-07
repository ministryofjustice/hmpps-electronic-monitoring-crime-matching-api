package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch

import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.EmailData
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.EmailIngestionOutcome
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmail
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmailAttachment
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmailAttachmentIngestionError
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchIngestionAttempt
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.IngestionStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.validation.EmailAttachmentIngestionError
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal.EmailIngestionPreparation

@Service
class CrimeBatchEmailIngestionService(
  private val entityManager: EntityManager,
  private val crimeBatchService: CrimeBatchService,
) {
  @Transactional
  fun persistIngestion(preparation: EmailIngestionPreparation): EmailIngestionOutcome {
    val persistedAttempt = preparation.crimeBatchIngestionAttempt
    entityManager.persist(persistedAttempt)
    val outcome = preparation.ingestionOutcome

    if (outcome.ingestionStatus == IngestionStatus.SUCCESSFUL || outcome.ingestionStatus == IngestionStatus.PARTIAL) {
      val attachment = persistedAttempt.crimeBatchEmail
        ?.crimeBatchEmailAttachments
        ?.singleOrNull()
        ?: throw IllegalStateException("Expected exactly one persisted email attachment for successful ingestion")

      val crimeBatch = crimeBatchService.createCrimeBatch(outcome.records, attachment)

      return outcome.copy(
        batchId = crimeBatch.batchId,
        crimeBatchId = crimeBatch.id.toString(),
      )
    }

    return outcome
  }

  fun createCrimeBatchIngestionAttempt(bucketName: String, objectKey: String): CrimeBatchIngestionAttempt = CrimeBatchIngestionAttempt(
    bucket = bucketName,
    objectName = objectKey,
  )

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
