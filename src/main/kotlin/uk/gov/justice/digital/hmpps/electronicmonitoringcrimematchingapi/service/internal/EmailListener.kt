package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.EmailData
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.EmailIngestionOutcome
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.EmailReceivedMessage
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.ParseResult
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.SqsMessage
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmail
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmailIngestionError
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchIngestionAttempt
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeBatchEmailIngestionErrorType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.IngestionStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.MatchingNotificationService
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch.CrimeBatchCsvService
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch.CrimeBatchEmailIngestionService
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch.CrimeBatchService
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.outbox.EmailOutboxService

@Service
class EmailListener(
  private val mapper: ObjectMapper,
  private val s3Service: S3Service,
  private val crimeBatchCsvService: CrimeBatchCsvService,
  private val crimeBatchEmailIngestionService: CrimeBatchEmailIngestionService,
  private val crimeBatchService: CrimeBatchService,
  private val emailOutboxService: EmailOutboxService,
  private val emailParserService: EmailParserService,
  private val matchingNotificationService: MatchingNotificationService,
  private val metricsService: MetricsService,
  transactionManager: PlatformTransactionManager,
) {

  private val transactionTemplate = TransactionTemplate(transactionManager)

  @SqsListener("email", factory = "hmppsQueueContainerFactoryProxy")
  fun receiveEmailNotification(message: SqsMessage) {
    // Map message contents
    val emailReceivedMessage: EmailReceivedMessage = mapper.readValue(message.Message)

    // Get S3 details from message
    val messageId = message.MessageId
    val bucketName = emailReceivedMessage.receipt.action.bucketName
    val objectKey = emailReceivedMessage.receipt.action.objectKey

    // Get email file from S3
    val emailFile = s3Service.getObject(messageId, objectKey, bucketName)

    // Extract email details (network I/O is performed outside the transaction below)
    val emailData = emailFile.use { emailParserService.extractEmailData(it) }

    // Persist the ingestion outcome and durably record the email intent in one transaction,
    // so an email is enqueued if and only if the ingestion outcome is committed.
    val ingestionOutcome = requireNotNull(
      transactionTemplate.execute {
        val outcome = processEmail(emailData, bucketName, objectKey)
        emailOutboxService.enqueue(outcome)
        outcome
      },
    )

    // Record ingestion outcome
    metricsService.recordOutcome(ingestionOutcome)

    // Published only after the ingestion transaction has committed.
    if (ingestionOutcome.ingestionStatus == IngestionStatus.SUCCESSFUL || ingestionOutcome.ingestionStatus == IngestionStatus.PARTIAL) {
      matchingNotificationService.publishMatchingRequest(ingestionOutcome.crimeBatchId)
    }
  }

  private fun processEmail(emailData: EmailData, bucketName: String, objectKey: String): EmailIngestionOutcome {
    // Initialise ingestion attempt
    val crimeBatchIngestionAttempt = crimeBatchEmailIngestionService.createCrimeBatchIngestionAttempt(bucketName, objectKey)

    // Initialise crime batch email
    val crimeBatchEmail = crimeBatchEmailIngestionService.createCrimeBatchEmail(emailData, crimeBatchIngestionAttempt)
      .also { crimeBatchIngestionAttempt.crimeBatchEmail = it }

    validateAttachment(emailData)?.let {
      saveIngestionAttemptError(it, crimeBatchIngestionAttempt, crimeBatchEmail)
      return EmailIngestionOutcome(
        emailData = emailData,
        ingestionStatus = IngestionStatus.FAILED,
        errorType = it,
      )
    }

    // Parse csv rows
    val attachment = emailData.attachments.single()
    val parseResult = attachment.inputStream.use { crimeBatchCsvService.parseCsvFile(it) }

    val crimeBatchEmailAttachment = crimeBatchEmailIngestionService.createCrimeBatchEmailAttachment(
      attachment.name,
      parseResult.recordCount,
      crimeBatchEmail,
    )

    validateBatch(parseResult)?.let {
      crimeBatchEmail.crimeBatchEmailAttachments += crimeBatchEmailAttachment
      saveIngestionAttemptError(it, crimeBatchIngestionAttempt, crimeBatchEmail)
      return EmailIngestionOutcome(
        emailData = emailData,
        ingestionStatus = IngestionStatus.FAILED,
        errorType = it,
      )
    }

    val attachmentIngestionErrors = parseResult.errors.map { error ->
      crimeBatchEmailIngestionService.createCrimeBatchEmailAttachmentIngestionError(
        error,
        crimeBatchEmailAttachment,
      )
    }
    crimeBatchEmailAttachment.crimeBatchEmailAttachmentIngestionErrors += attachmentIngestionErrors

    crimeBatchEmail.crimeBatchEmailAttachments += crimeBatchEmailAttachment

    crimeBatchEmailIngestionService.saveCrimeBatchIngestionAttempt(crimeBatchIngestionAttempt)

    // Create batch if records present
    if (parseResult.records.isNotEmpty()) {
      val crimeBatch = crimeBatchService.createCrimeBatch(parseResult.records, crimeBatchEmailAttachment)
      val policeForce = parseResult.records.first().policeForce
      val batchId = crimeBatch.batchId
      val crimeBatchId = crimeBatch.id.toString()
      val status = if (parseResult.errors.isEmpty()) IngestionStatus.SUCCESSFUL else IngestionStatus.PARTIAL
      return EmailIngestionOutcome(
        batchId = batchId,
        crimeBatchId = crimeBatchId,
        policeForce = policeForce.label,
        errors = parseResult.errors,
        emailData = emailData,
        records = parseResult.records,
        recordCount = parseResult.recordCount,
        ingestionStatus = status,
      )
    }

    return EmailIngestionOutcome(
      emailData = emailData,
      errors = parseResult.errors,
      recordCount = parseResult.recordCount,
      errorType = CrimeBatchEmailIngestionErrorType.ALL_RECORDS_FAILED,
      ingestionStatus = IngestionStatus.ERROR,
    )
  }

  private fun validateAttachment(emailData: EmailData): CrimeBatchEmailIngestionErrorType? = when (emailData.attachments.size) {
    1 -> null
    0 -> CrimeBatchEmailIngestionErrorType.INVALID_ATTACHMENT
    else -> CrimeBatchEmailIngestionErrorType.INVALID_ATTACHMENT
  }

  private fun validateBatch(parseResult: ParseResult): CrimeBatchEmailIngestionErrorType? {
    val forces = parseResult.records.map { it.policeForce }.distinct()
    if (forces.size > 1) return CrimeBatchEmailIngestionErrorType.MULTIPLE_POLICE_FORCES

    val batchIds = parseResult.records.map { it.batchId }.distinct()
    if (batchIds.size > 1) return CrimeBatchEmailIngestionErrorType.MULTIPLE_BATCH_IDS

    return null
  }

  private fun saveIngestionAttemptError(
    errorType: CrimeBatchEmailIngestionErrorType,
    crimeBatchIngestionAttempt: CrimeBatchIngestionAttempt,
    crimeBatchEmail: CrimeBatchEmail,
  ) {
    val crimeBatchEmailIngestionError = CrimeBatchEmailIngestionError(
      errorType = errorType,
      crimeBatchEmail = crimeBatchEmail,
    )
    crimeBatchEmail.crimeBatchEmailIngestionError = crimeBatchEmailIngestionError
    crimeBatchIngestionAttempt.crimeBatchEmail = crimeBatchEmail
    crimeBatchEmailIngestionService.saveCrimeBatchIngestionAttempt(crimeBatchIngestionAttempt)
  }
}
