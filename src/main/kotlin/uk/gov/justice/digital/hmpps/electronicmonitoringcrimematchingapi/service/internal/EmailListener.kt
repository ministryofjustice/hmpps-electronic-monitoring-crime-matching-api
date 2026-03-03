package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.EmailReceivedMessage
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.SqsMessage
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmailAttachmentIngestionError
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmailIngestionError
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.BatchIngestionErrorType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.validation.BatchIngestionError
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
    try {
      // Map message contents
      val emailReceivedMessage: EmailReceivedMessage = mapper.readValue(message.Message)

      // Get S3 details from message
      val messageId = message.MessageId
      val bucketName = emailReceivedMessage.receipt.action.bucketName
      val objectKey = emailReceivedMessage.receipt.action.objectKey

      // Get email file from S3
      val emailFile = s3Service.getObject(messageId.toString(), objectKey, bucketName)

      // Initialise ingestion attempt
      val crimeBatchIngestionAttempt = crimeBatchEmailIngestionService.createCrimeBatchIngestionAttempt(bucketName, objectKey)

      // Extract email details
      val emailData = emailFile.use { emailParserService.extractEmailData(it) }

      // Initialise crime batch email
      val crimeBatchEmail = crimeBatchEmailIngestionService.createCrimeBatchEmail(emailData, crimeBatchIngestionAttempt)
      crimeBatchIngestionAttempt.crimeBatchEmail = crimeBatchEmail

      val ingestionErrors = mutableListOf<BatchIngestionError>()

      try {
        if (emailData.attachments.isEmpty() || emailData.attachments.size > 1) {
          val error = BatchIngestionError(
            errorType = BatchIngestionErrorType.INVALID_ATTACHMENT,
          )
          ingestionErrors.add(error)
        }

        // Parse csv rows
        val csvData = emailData.attachments[0].inputStream
        val parseResult = csvData.use { crimeBatchCsvService.parseCsvFile(it) }

        if (parseResult.records.isNotEmpty() && parseResult.records.map { it.policeForce }.distinct().size != 1) {
          val error = BatchIngestionError(
            errorType = BatchIngestionErrorType.MULTIPLE_POLICE_FORCES,
          )
          ingestionErrors.add(error)
        }

        if (parseResult.records.isNotEmpty() && parseResult.records.map { it.batchId }.distinct().size != 1) {
          val error = BatchIngestionError(
            errorType = BatchIngestionErrorType.MULTIPLE_BATCH_IDS,
          )
          ingestionErrors.add(error)
        }

        val crimeBatchEmailAttachment = crimeBatchEmailIngestionService.createCrimeBatchEmailAttachment(
          emailData.attachments[0].name,
          parseResult.recordCount,
          crimeBatchEmail,
        )

        val attachmentErrors = mutableListOf<CrimeBatchEmailAttachmentIngestionError>()
        for (error in parseResult.errors) {
          attachmentErrors.add(
            crimeBatchEmailIngestionService.createCrimeBatchEmailAttachmentIngestionError(
              error,
              crimeBatchEmailAttachment,
            ),
          )
        }

        crimeBatchEmailAttachment.crimeBatchEmailAttachmentIngestionErrors.addAll(attachmentErrors)
        crimeBatchEmail.crimeBatchEmailAttachments.add(crimeBatchEmailAttachment)

        crimeBatchEmailIngestionService.saveCrimeBatchIngestionAttempt(crimeBatchIngestionAttempt)

        // Create batch if records present
        if (parseResult.records.isNotEmpty()) {
          val crimeBatch = crimeBatchService.createCrimeBatch(parseResult.records, crimeBatchEmailAttachment)

          // Emit success email
          if (attachmentErrors.isEmpty()) {
            val policeForce = parseResult.records.first().policeForce
            emailNotificationService.sendSuccessfulIngestionEmail(
              crimeBatch.batchId,
              policeForce,
              emailData,
              parseResult.records,
            )
          }
        }

      } catch (e: ValidationException) {
        if (ingestionErrors.isNotEmpty()) {
          val crimeBatchEmailIngestionError = CrimeBatchEmailIngestionError(
            errorType = ingestionErrors[0].errorType,
            crimeBatchEmail = crimeBatchEmail,
          )
          crimeBatchEmail.crimeBatchEmailIngestionError = crimeBatchEmailIngestionError
          crimeBatchIngestionAttempt.crimeBatchEmail = crimeBatchEmail
          crimeBatchEmailIngestionService.saveCrimeBatchIngestionAttempt(crimeBatchIngestionAttempt)
        }
        // Emit failure email
      }
    } catch (e: Exception) {
      throw ValidationException("Failed to process email: ${e.message}")
    }
  }
}
