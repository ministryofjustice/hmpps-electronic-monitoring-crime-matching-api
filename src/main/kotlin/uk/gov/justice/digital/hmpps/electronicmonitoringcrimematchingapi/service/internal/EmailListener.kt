package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
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
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.MatchingPublishState
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.MatchingNotificationService
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch.CrimeBatchCsvService
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch.CrimeBatchEmailIngestionService
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch.CrimeBatchService
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.outbox.EmailOutboxService
import java.util.UUID

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

  private val log = LoggerFactory.getLogger(this::class.java)
  private val transactionTemplate = TransactionTemplate(transactionManager)

  /** Internal carrier that pairs the ingestion outcome with its persisted attempt entity. */
  private data class ProcessEmailResult(
    val outcome: EmailIngestionOutcome,
    val attempt: CrimeBatchIngestionAttempt,
  )

  @SqsListener("email", factory = "hmppsQueueContainerFactoryProxy")
  fun receiveEmailNotification(message: SqsMessage) {
    val emailReceivedMessage: EmailReceivedMessage = mapper.readValue(message.Message)
    val messageId = message.MessageId
    val bucketName = emailReceivedMessage.receipt.action.bucketName
    val objectKey = emailReceivedMessage.receipt.action.objectKey

    // Idempotency check: (bucket, objectKey) uniquely identifies the source email.
    // A prior attempt means this is a duplicate SQS delivery — handle without re-ingesting.
    val existingAttempt = crimeBatchEmailIngestionService.findIngestionAttemptBySource(bucketName, objectKey)
    if (existingAttempt != null) {
      handleDuplicate(existingAttempt)
      return
    }

    // First-time ingestion: fetch the email from S3 and parse it outside the transaction.
    val emailFile = s3Service.getObject(messageId, objectKey, bucketName)
    val emailData = emailFile.use { emailParserService.extractEmailData(it) }

    // Persist the ingestion outcome, the email outbox intent, and the initial publish state
    // atomically so all three are committed or none are.
    val ingestionResult = requireNotNull(
      transactionTemplate.execute {
        val result = processEmail(emailData, bucketName, objectKey)
        emailOutboxService.enqueue(result.outcome)

        // Decide publish state immediately: NOT_REQUIRED for non-publishable outcomes so
        // duplicate deliveries never retry a publish that will never happen.
        val isPublishable = isPublishable(result.outcome)
        result.attempt.matchingPublishState = if (isPublishable) MatchingPublishState.PENDING_OR_UNCONFIRMED else MatchingPublishState.NOT_REQUIRED
        if (isPublishable) {
          result.attempt.crimeBatchId = result.outcome.crimeBatchId.toUuidOrNull()
        }
        // Explicit save so the state and crimeBatchId fields are persisted atomically.
        crimeBatchEmailIngestionService.saveCrimeBatchIngestionAttempt(result.attempt)
        result
      },
    )

    metricsService.recordOutcome(ingestionResult.outcome)

    if (isPublishable(ingestionResult.outcome)) {
      publishAndMarkPublished(ingestionResult.attempt, ingestionResult.outcome.crimeBatchId)
    }
  }

  /**
   * Handles a duplicate SQS delivery (the source email was already ingested).
   *
   * - [MatchingPublishState.PUBLISHED] / [MatchingPublishState.NOT_REQUIRED]: prior state
   *   is known — safe no-op; no new ingestion or outbox rows are created.
   * - [MatchingPublishState.PENDING_OR_UNCONFIRMED]: retry [publishMatchingRequest] for
   *   at-least-once delivery, including after a crash that prevented the prior outcome from
   *   being persisted.
   */
  private fun handleDuplicate(existingAttempt: CrimeBatchIngestionAttempt) {
    log.warn(
      "Duplicate SQS delivery for bucket={} objectKey={} ingestionAttempt={} publishState={}",
      existingAttempt.bucket,
      existingAttempt.objectName,
      existingAttempt.id,
      existingAttempt.matchingPublishState,
    )
    when (existingAttempt.matchingPublishState) {
      MatchingPublishState.PUBLISHED, MatchingPublishState.NOT_REQUIRED -> {
        // Prior state known — no-op.
      }
      MatchingPublishState.PENDING_OR_UNCONFIRMED -> {
        // Retry from the retryable state to preserve at-least-once delivery.
        existingAttempt.crimeBatchId?.let { crimeBatchId ->
          publishAndMarkPublished(existingAttempt, crimeBatchId.toString())
        }
      }
    }
  }

  /**
   * Publishes the matching request then best-effort persists [MatchingPublishState.PUBLISHED].
   *
   * If the state update fails the exception is caught and logged: the publish already succeeded,
   * so rethrowing to SQS would only cause a duplicate publish. Leaving state as
   * [MatchingPublishState.PENDING_OR_UNCONFIRMED] is safer and still delivers at-least-once.
   */
  private fun publishAndMarkPublished(attempt: CrimeBatchIngestionAttempt, crimeBatchId: String) {
    matchingNotificationService.publishMatchingRequest(crimeBatchId)
    runCatching {
      crimeBatchEmailIngestionService.markMatchingPublishState(attempt.id, MatchingPublishState.PUBLISHED)
    }.onFailure {
      log.warn("Failed to mark matching publish state PUBLISHED for ingestion attempt {}", attempt.id, it)
    }
  }

  private fun isPublishable(outcome: EmailIngestionOutcome): Boolean = outcome.ingestionStatus == IngestionStatus.SUCCESSFUL || outcome.ingestionStatus == IngestionStatus.PARTIAL

  private fun String.toUuidOrNull(): UUID? = runCatching { UUID.fromString(this) }.getOrNull()

  private fun processEmail(emailData: EmailData, bucketName: String, objectKey: String): ProcessEmailResult {
    // Initialise ingestion attempt
    val crimeBatchIngestionAttempt = crimeBatchEmailIngestionService.createCrimeBatchIngestionAttempt(bucketName, objectKey)

    // Initialise crime batch email
    val crimeBatchEmail = crimeBatchEmailIngestionService.createCrimeBatchEmail(emailData, crimeBatchIngestionAttempt)
      .also { crimeBatchIngestionAttempt.crimeBatchEmail = it }

    validateAttachment(emailData)?.let {
      saveIngestionAttemptError(it, crimeBatchIngestionAttempt, crimeBatchEmail)
      return ProcessEmailResult(
        outcome = EmailIngestionOutcome(emailData = emailData, ingestionStatus = IngestionStatus.FAILED, errorType = it),
        attempt = crimeBatchIngestionAttempt,
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
      return ProcessEmailResult(
        outcome = EmailIngestionOutcome(emailData = emailData, ingestionStatus = IngestionStatus.FAILED, errorType = it),
        attempt = crimeBatchIngestionAttempt,
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
      return ProcessEmailResult(
        outcome = EmailIngestionOutcome(
          batchId = batchId,
          crimeBatchId = crimeBatchId,
          policeForce = policeForce.label,
          errors = parseResult.errors,
          emailData = emailData,
          records = parseResult.records,
          recordCount = parseResult.recordCount,
          ingestionStatus = status,
        ),
        attempt = crimeBatchIngestionAttempt,
      )
    }

    return ProcessEmailResult(
      outcome = EmailIngestionOutcome(
        emailData = emailData,
        errors = parseResult.errors,
        recordCount = parseResult.recordCount,
        errorType = CrimeBatchEmailIngestionErrorType.ALL_RECORDS_FAILED,
        ingestionStatus = IngestionStatus.ERROR,
      ),
      attempt = crimeBatchIngestionAttempt,
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
