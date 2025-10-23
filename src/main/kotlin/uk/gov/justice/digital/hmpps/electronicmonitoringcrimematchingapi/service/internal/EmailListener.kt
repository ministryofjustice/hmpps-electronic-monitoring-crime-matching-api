package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.extractAttachment
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.MessageBody
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.SqsMessage
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch.CrimeBatchService

@Service
class EmailListener(
  private val mapper: ObjectMapper,
  private val s3Service: S3Service,
  private val crimeBatchService: CrimeBatchService,
) {

  @SqsListener("email", factory = "hmppsQueueContainerFactoryProxy")
  fun receiveEmailNotification(message: SqsMessage) {
    try {
      // Map message contents
      val messageBody: MessageBody = mapper.readValue(message.Message)

      // Get S3 object key and bucket from message
      val bucketName = messageBody.receipt.action.bucketName
      val objectKey = messageBody.receipt.action.objectKey

      // Get email file from S3
      val emailFile = s3Service.getObject(objectKey, bucketName)

      // Extract attachment from file
      val csvData = emailFile.use { extractAttachment(it) }

      // Parse csv rows and insert into DB
      crimeBatchService.ingestCsvData(csvData)
    } catch (e: Exception) {
      throw ValidationException("Failed to process email: ${e.message}")
    }
  }
}
