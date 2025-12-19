package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.extractEmailData
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.EmailReceivedMessage
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.SqsMessage
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmailAttachmentIngestionError
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

      // Extract email details
      val emailData = emailFile.use { extractEmailData(it) }

      // Parse csv rows
      val csvData = emailData.attachment.inputStream
      val parseResult = csvData.use { crimeBatchCsvService.parseCsvFile(it) }

      val crimeBatchIngestionAttempt = crimeBatchEmailIngestionService.createCrimeBatchIngestionAttempt(bucketName, objectKey)

      val crimeBatchEmail = crimeBatchEmailIngestionService.createCrimeBatchEmail(emailData, crimeBatchIngestionAttempt)
      crimeBatchIngestionAttempt.crimeBatchEmail = crimeBatchEmail

      val crimeBatchEmailAttachment = crimeBatchEmailIngestionService.createCrimeBatchEmailAttachment(emailData.attachment.name, parseResult.recordCount, crimeBatchEmail)

      // Save ingestion errors from parse results
      val ingestionErrors = mutableListOf<CrimeBatchEmailAttachmentIngestionError>()
      for (error in parseResult.errors) {
        ingestionErrors.add(crimeBatchEmailIngestionService.createCrimeBatchEmailIngestionError(error, crimeBatchEmailAttachment))
      }
      crimeBatchEmailAttachment.crimeBatchEmailAttachmentIngestionErrors.addAll(ingestionErrors)

      crimeBatchEmail.crimeBatchEmailAttachments.add(crimeBatchEmailAttachment)

      crimeBatchEmailIngestionService.saveCrimeBatchIngestionAttempt(crimeBatchIngestionAttempt)

      crimeBatchService.createCrimeBatch(parseResult.records, crimeBatchEmailAttachment)
    } catch (e: Exception) {
      throw ValidationException("Failed to process email: ${e.message}")
    }
  }
}
