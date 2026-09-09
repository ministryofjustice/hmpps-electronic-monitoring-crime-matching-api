package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.EmailData
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.EmailIngestionOutcome
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmail
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmailAttachment
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmailAttachmentIngestionError
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchIngestionAttempt
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.PublishMatchingOutbox
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.IngestionStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PublishMatchingState
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.validation.EmailAttachmentIngestionError
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeBatchIngestionAttemptRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.publishMatching.PublishMatchingOutboxRepository

@Service
class CrimeBatchEmailIngestionService(
  private val crimeBatchIngestionAttemptRepository: CrimeBatchIngestionAttemptRepository,
  private val crimeBatchService: CrimeBatchService,
  private val publishMatchingOutboxRepository: PublishMatchingOutboxRepository,
) {
  @Transactional
  fun persistIngestion(
    ingestionAttempt: CrimeBatchIngestionAttempt,
    outcome: EmailIngestionOutcome,
  ): EmailIngestionOutcome {
    crimeBatchIngestionAttemptRepository.save(ingestionAttempt)

    if (outcome.ingestionStatus == IngestionStatus.SUCCESSFUL || outcome.ingestionStatus == IngestionStatus.PARTIAL) {
      val crimeBatch = crimeBatchService.createCrimeBatch(
        outcome.records,
        ingestionAttempt.crimeBatchEmail!!.crimeBatchEmailAttachments.first(),
      )

      savePublishMatchingOutboxState(ingestionAttempt, PublishMatchingState.PENDING_OR_UNCONFIRMED)

      return outcome.copy(
        batchId = crimeBatch.batchId,
        crimeBatchId = crimeBatch.id.toString(),
      )
    }

    return outcome
  }

  private fun savePublishMatchingOutboxState(ingestionAttempt: CrimeBatchIngestionAttempt, state: PublishMatchingState) {
    publishMatchingOutboxRepository.save(
      PublishMatchingOutbox(
        crimeBatchIngestionAttempt = ingestionAttempt,
        state = state,
      ),
    )
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
