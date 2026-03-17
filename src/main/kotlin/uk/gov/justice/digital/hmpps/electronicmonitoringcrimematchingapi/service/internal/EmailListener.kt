package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.EmailData
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.EmailReceivedMessage
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.ParseResult
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.SqsMessage
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmail
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmailIngestionError
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchIngestionAttempt
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeBatchEmailIngestionErrorType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch.CrimeBatchCsvService
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch.CrimeBatchEmailIngestionService
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch.CrimeBatchService

@Service
class EmailListener(
  private val mapper: ObjectMapper,
  private val s3Service: S3Service,
  private val crimeBatchCsvService: CrimeBatchCsvService,
  private val crimeBatchEmailIngestionService: CrimeBatchEmailIngestionService,
  private val crimeBatchService: CrimeBatchService,
  private val emailNotificationService: EmailNotificationService,
  private val emailParserService: EmailParserService,
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  @SqsListener("email", factory = "hmppsQueueContainerFactoryProxy")
  fun receiveEmailNotification(message: SqsMessage) {
    var emailData: EmailData? = null
    var crimeBatchIngestionAttempt: CrimeBatchIngestionAttempt? = null

    try {
      // Map message contents
      val emailReceivedMessage: EmailReceivedMessage = mapper.readValue(message.Message)

      // Get S3 details from message
      val messageId = message.MessageId
      val bucketName = emailReceivedMessage.receipt.action.bucketName
      val objectKey = emailReceivedMessage.receipt.action.objectKey

      // Get email file from S3
      val emailFile = s3Service.getObject(messageId, objectKey, bucketName)

      // Initialise ingestion attempt
      crimeBatchIngestionAttempt = crimeBatchEmailIngestionService.createCrimeBatchIngestionAttempt(bucketName, objectKey)

      // Extract email details
      emailData = emailFile.use { emailParserService.extractEmailData(it) }

      // Initialise crime batch email
      val crimeBatchEmail = crimeBatchEmailIngestionService.createCrimeBatchEmail(emailData, crimeBatchIngestionAttempt)
        .also { crimeBatchIngestionAttempt.crimeBatchEmail = it }

      validateAttachment(emailData)?.let {
        saveIngestionAttemptError(it, crimeBatchIngestionAttempt, crimeBatchEmail)
        try {
          emailNotificationService.sendFailedIngestionEmail(emailData = emailData, errorType = it.name)
        } catch (notifyEx: Exception) {
          log.warn("Failed to send failed ingestion notification email: ${notifyEx.message}", notifyEx)
        }
        return
      }

      // Parse csv rows
      val attachment = emailData.attachments.single()
      val parseResult = attachment.inputStream.use { crimeBatchCsvService.parseCsvFile(it) }

      validateBatch(parseResult)?.let {
        saveIngestionAttemptError(it, crimeBatchIngestionAttempt, crimeBatchEmail)
        try {
          emailNotificationService.sendFailedIngestionEmail(emailData = emailData, errorType = it.name)
        } catch (notifyEx: Exception) {
          log.warn("Failed to send failed ingestion notification email: ${notifyEx.message}", notifyEx)
        }
        return
      }

      val crimeBatchEmailAttachment = crimeBatchEmailIngestionService.createCrimeBatchEmailAttachment(
        attachment.name,
        parseResult.recordCount,
        crimeBatchEmail,
      )

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

        // Emit success email
        if (attachmentIngestionErrors.isEmpty()) {
          emailNotificationService.sendSuccessfulIngestionEmail(
            crimeBatch.batchId,
            policeForce,
            emailData,
            parseResult.records,
          )
        } else {
          emailNotificationService.sendPartialIngestionEmail(
            crimeBatch.batchId,
            policeForce,
            emailData,
            parseResult.errors,
            totalCount = parseResult.records.size + parseResult.errors.size,
          )
        }
      }
    } catch (e: Exception) {
      try {
        emailData?.let { emailNotificationService.sendFailedIngestionEmail(emailData = it) }
      } catch (notifyEx: Exception) {
        log.warn("Failed to send failed ingestion notification email: ${notifyEx.message}", notifyEx)
      }
      throw ValidationException("Failed to process email: ${e.message}")
    }
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
