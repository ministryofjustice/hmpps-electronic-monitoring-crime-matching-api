package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config.notify.NotifyProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeRecordRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.EmailData
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.FailedRecord
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.ParseResult
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce
import uk.gov.service.notify.NotificationClient
import java.time.LocalDate
import org.slf4j.LoggerFactory 

@Service
class EmailNotificationService(
  private val notifyClient: NotificationClient,
  private val properties: NotifyProperties,
) {

   private val log = LoggerFactory.getLogger(this::class.java)

  fun sendSuccessfulIngestionEmail(
    batchId: String,
    policeForce: PoliceForce,
    emailData: EmailData,
    records: List<CrimeRecordRequest>,
  ) {
    val emailAddresses = listOf(emailData.sender, emailData.originalSender)
    val csvBytes = records.toCsv().toByteArray()

    val personalisation = hashMapOf<String, Any>()
    personalisation["fileName"] = emailData.attachment.name
    personalisation["ingestionDate"] = LocalDate.now().toString()
    personalisation["batchId"] = batchId
    personalisation["policeForce"] = policeForce.name
    personalisation["linkToFile"] = NotificationClient.prepareUpload(csvBytes, emailData.attachment.name)

    for (emailAddress in emailAddresses) {
      sendEmail(properties.successfulIngestionTemplateId, emailAddress, personalisation, batchId)
    }
  }

  fun sendFailedIngestionEmail(
    batchId: String,
    policeForce: String,
    emailData: EmailData,
    parseResult: ParseResult,
    errorSummary: String,
  ) {
    val emailAddresses = listOf(emailData.sender, emailData.originalSender)

    val csvErrorBytes = buildFailedRecordsCsv(parseResult.failedRecords).toByteArray()

    val personalisation = hashMapOf<String, Any>()
    personalisation["fileName"] = emailData.attachment.name
    personalisation["ingestionDate"] = LocalDate.now().toString()
    personalisation["batchId"] = batchId
    personalisation["policeForce"] = "Unknown"
    personalisation["totalRecords"] = parseResult.recordCount
    personalisation["failedCount"] = parseResult.failedRecords.size
    personalisation["errorSummary"] = errorSummary
    personalisation["linkToFile"] = NotificationClient.prepareUpload(csvErrorBytes, emailData.attachment.name)

    for (emailAddress in emailAddresses) {
      sendEmail(properties.failedIngestionTemplateId, emailAddress, personalisation, "batchId")
    }
  }

  fun sendPartialIngestionEmail(
    batchId: String,
    policeForce: PoliceForce,
    emailData: EmailData,
    records: List<CrimeRecordRequest>,
    parseResult: ParseResult,
    successCount: Int,
    failedCount: Int,
    totalRecords: Int,
    errorSummary: String,
  ) {

    val csvBytes = buildFailedRecordsCsv(parseResult.failedRecords).toByteArray()
    val originalFileName = emailData.attachment.name
    val errorFileName = "failed_ingestion_$originalFileName"
    val uploadFile = NotificationClient.prepareUpload(csvBytes, errorFileName)

    val personalisation = hashMapOf<String, Any>()
    personalisation["fileName"] = emailData.attachment.name
    personalisation["ingestionDate"] = LocalDate.now().toString()
    personalisation["batchId"] = batchId
    personalisation["policeForce"] = policeForce.name
    personalisation["totalRecords"] = totalRecords.toString()
    personalisation["successCount"] = successCount.toString()
    personalisation["failedCount"] = failedCount.toString()
    personalisation["errorSummary"] = errorSummary
    personalisation["linkToFile"] = uploadFile

    val emailAddresses = listOf(emailData.sender, emailData.originalSender)
    for (emailAddress in emailAddresses) {
      sendEmail(properties.partialIngestionTemplateId, emailAddress, personalisation, batchId)
    }
  }

  private fun buildFailedRecordsCsv(failedRecords: List<FailedRecord>): String = buildString {
    if (failedRecords.isEmpty()) {
      append("No failed records\n")
      return@buildString
    }
    for (failedRecord in failedRecords) {
      append(failedRecord.originalCsvRow ?: "")
      append(",")
      append(failedRecord.errorMessage.replace(",", ";"))
      append("\n")
    }
  }

  private fun List<CrimeRecordRequest>.toCsv(): String = buildString {
    this@toCsv.forEach { r ->
      appendCsvRow(
        r.policeForce.value,
        r.crimeTypeId.name,
        r.crimeTypeId.value,
        r.batchId,
        r.crimeReference,
        r.crimeDateTimeFrom,
        r.crimeDateTimeTo,
        r.easting,
        r.northing,
        r.latitude,
        r.longitude,
        r.crimeText,
      )
    }
  }

  private fun StringBuilder.appendCsvRow(vararg fields: Any?) {
    append(
      fields.joinToString(",") { it?.toString().orEmpty() },
    )
    append("\n")
  }

  private fun sendEmail(
    templateId: String,
    emailAddress: String,
    personalisation: Map<String, Any>,
    reference: String,
  ) {
     if (properties.enabled) {
      notifyClient.sendEmail(
        templateId,
        emailAddress,
        personalisation,
        reference,
      )
      log.debug("Email sent successfully to $emailAddress with template: $templateId")
    }
  }
}
