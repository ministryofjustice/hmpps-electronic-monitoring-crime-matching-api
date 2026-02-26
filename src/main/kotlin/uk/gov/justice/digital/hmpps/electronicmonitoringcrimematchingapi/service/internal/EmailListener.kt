package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.EmailReceivedMessage
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.FailedRecord
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.ParseResult
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.SqsMessage
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

      // Extract email details
      val emailData = emailFile.use { emailParserService.extractEmailData(it) }

      // Parse csv rows
      val csvData = emailData.attachment.inputStream
      val parseResult = csvData.use { crimeBatchCsvService.parseCsvFile(it) }

      for (error in parseResult.errors) {
        log.debug("Crime data violation found: $error")
      }

      val crimeBatchIngestionAttempt = crimeBatchEmailIngestionService.createCrimeBatchIngestionAttempt(bucketName, objectKey)

      val crimeBatchEmail = crimeBatchEmailIngestionService.createCrimeBatchEmail(emailData, crimeBatchIngestionAttempt)
      crimeBatchIngestionAttempt.crimeBatchEmail = crimeBatchEmail

      val successCount = parseResult.records.size
      val failedCount = parseResult.failedRecords.size

      val crimeBatchEmailAttachment = crimeBatchEmailIngestionService.createCrimeBatchEmailAttachment(
        emailData.attachment.name,
        parseResult.recordCount,
        successCount,
        failedCount,
        crimeBatchEmail
      )
      crimeBatchEmail.crimeBatchEmailAttachments.add(crimeBatchEmailAttachment)

      crimeBatchEmailIngestionService.saveCrimeBatchIngestionAttempt(crimeBatchIngestionAttempt)

      if (parseResult.errors.isEmpty() && parseResult.records.isNotEmpty()) {
        val crimeBatch = crimeBatchService.createCrimeBatch(parseResult.records, crimeBatchEmailAttachment)
        val policeForce = parseResult.records.first().policeForce
        
        emailNotificationService.sendSuccessfulIngestionEmail(
          crimeBatch.batchId,
          policeForce,
          emailData,
          parseResult.records,
        )
      } else if (parseResult.records.isEmpty()) {
        val policeForce = "Unknown"
        val errorSummary = buildErrorSummary(parseResult)

        emailNotificationService.sendFailedIngestionEmail(
          "batchId",
          "Unknown",
          emailData,
          parseResult,
          errorSummary,
        )
        log.info("Failed ingestion - ${parseResult.failedRecords.size} of ${parseResult.recordCount} records failed validation")
      } else {
        val crimeBatch = crimeBatchService.createCrimeBatch(parseResult.records, crimeBatchEmailAttachment)
        val policeForce = parseResult.records.first().policeForce
        val errorSummary = buildErrorSummary(parseResult)

        emailNotificationService.sendPartialIngestionEmail(
          crimeBatch.batchId,
          policeForce,
          emailData,
          parseResult.records,
          parseResult,
          successCount,
          failedCount,
          parseResult.recordCount,
          errorSummary,
        )
        log.info("Partial ingestion - $successCount of ${parseResult.recordCount} records ingested and $failedCount records failed validation")
      }
    } catch (e: Exception) {
      throw ValidationException("Failed to process email: ${e.message}")
    }
  }

  private fun buildErrorSummary(parseResult: ParseResult): String {
    if (parseResult.failedRecords.isEmpty()) {
      return parseResult.errors.take(5).joinToString("\n") { "-$it" }
    }

    val top_5_errors = parseResult.failedRecords.take(5).joinToString("\n") { record ->
      "* Row ${record.rowNumber}: ${record.errorMessage}"
    }

    return if (parseResult.failedRecords.size > 5) {
      "$top_5_errors\n\nThere are ${parseResult.failedRecords.size} errors in total. Check the attached CSV for full details."
    } else {
      top_5_errors
    }
  }
}
