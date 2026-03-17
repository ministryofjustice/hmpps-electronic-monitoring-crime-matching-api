package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config.notify.NotifyProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeRecordRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.EmailData
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeBatchEmailAttachmentIngestionErrorType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.validation.EmailAttachmentIngestionError
import uk.gov.service.notify.NotificationClient
import java.time.LocalDate

@Service
class EmailNotificationService(
  private val notifyClient: NotificationClient,
  private val properties: NotifyProperties,
) {
  fun sendSuccessfulIngestionEmail(
    batchId: String,
    policeForce: PoliceForce,
    emailData: EmailData,
    records: List<CrimeRecordRequest>,
  ) {

    val emailAddresses = listOf(emailData.sender, emailData.originalSender)
    val csvBytes = records.toCsv().toByteArray()

    val personalisation = hashMapOf<String, Any>()
    personalisation["fileName"] = emailData.attachments.single().name
    personalisation["ingestionDate"] = LocalDate.now().toString()
    personalisation["batchId"] = batchId
    personalisation["policeForce"] = policeForce.name
    personalisation["linkToFile"] = NotificationClient.prepareUpload(csvBytes, emailData.attachments.single().name)

    for (emailAddress in emailAddresses) {
      sendEmail(properties.successfulIngestionTemplateId, emailAddress, personalisation, batchId)
    }
  }

  fun sendFailedIngestionEmail(
    emailData: EmailData,
    errorType: String? = null,
  ) {

    val personalisation = hashMapOf<String, Any>()
    personalisation["fileName"] = (emailData.attachments.firstOrNull()?.name ?: "" as String)
    personalisation["ingestionDate"] = LocalDate.now().toString()
    personalisation["batchId"] = "Unknown due to an error"
    personalisation["policeForce"] = "Unknown Force"
    personalisation["errorSummary"] = errorType ?: "Unknown error"
    personalisation["totalCount"] = 0

    val emailAddresses = listOf(emailData.sender)
    for (emailAddress in emailAddresses) {
      sendEmail(properties.failedIngestionTemplateId, emailAddress, personalisation, "batchId")
    }
  }

  fun sendPartialIngestionEmail(
    batchId: String,
    policeForce: PoliceForce,
    emailData: EmailData,
    errors: List<EmailAttachmentIngestionError>,
    totalCount: Int,
  ) {

    val successCount = totalCount - errors.size

    val personalisation = hashMapOf<String, Any>()
    personalisation["fileName"] = emailData.attachments.firstOrNull()?.name ?: ""
    personalisation["ingestionDate"] = LocalDate.now().toString()
    personalisation["batchId"] = batchId
    personalisation["policeForce"] = policeForce.name
    personalisation["errorSummary"] = buildInLineErrorSummary(errors)
    personalisation["totalCount"] = totalCount
    personalisation["successCount"] = totalCount - errors.size
    personalisation["failedCount"] = errors.size
    personalisation["linkToFile"] = NotificationClient.prepareUpload(buildErrorCsv(errors), "partial_ingestion_errors.csv")

    val emailAddresses = listOf(emailData.sender, emailData.originalSender)
    for (emailAddress in emailAddresses) {
      sendEmail(properties.partialIngestionTemplateId, emailAddress, personalisation, "batchId")
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

  private fun buildInLineErrorSummary(errors: List<EmailAttachmentIngestionError>): String = errors.take(5).joinToString("\n") { error ->
    "Row ${error.rowNumber}: ${error.errorType.message}" +
      (if (error.field != null) " (${error.field})" else "")
  }

  private fun buildErrorCsv(errors: List<EmailAttachmentIngestionError>): ByteArray = buildString {
    appendLine("Reference,Status,Error,Action Required")
    errors.forEach { error ->
      appendLine(
        "${error.crimeReference ?: ""},Failed, ${error.errorType.message},${actionRequired(error.errorType, error.field)}",
      )
    }
  }.toByteArray(Charsets.UTF_8)

  private fun actionRequired(
    errorType: CrimeBatchEmailAttachmentIngestionErrorType,
    field: String?,
  ): String = when (errorType) {
    CrimeBatchEmailAttachmentIngestionErrorType.INVALID_ENUM ->
      if (field?.lowercase()?.contains("policeForce") == true) {
        "Amend police force to a registered force"
      } else {
        "Amend crime type to a registered crime type"
      }
    CrimeBatchEmailAttachmentIngestionErrorType.MISSING_CRIME_REFERENCE ->
      "Provide the missing test reference"
    CrimeBatchEmailAttachmentIngestionErrorType.INVALID_DATE_FORMAT ->
      if (field?.lowercase()?.contains("from") == true) {
        "Amend from date/time to format yyyyMMddHHmmss"
      } else {
        "Amend to date/time to format yyyyMMddHHmmss"
      }
    CrimeBatchEmailAttachmentIngestionErrorType.CRIME_DATE_TIME_TO_AFTER_FROM ->
      "Ensure from date/time precedes to date/time"
    CrimeBatchEmailAttachmentIngestionErrorType.INVALID_LOCATION_DATA_RANGE ->
      "Co-ordinates outside of valid range"
    CrimeBatchEmailAttachmentIngestionErrorType.MISSING_LOCATION_DATA ->
      "Provide location data"
    CrimeBatchEmailAttachmentIngestionErrorType.INVALID_BATCH_ID_FORMAT,
    CrimeBatchEmailAttachmentIngestionErrorType.INVALID_BATCH_ID_DATE,
    CrimeBatchEmailAttachmentIngestionErrorType.INVALID_COLUMN_COUNT,
    CrimeBatchEmailAttachmentIngestionErrorType.INVALID_TEXT,
    CrimeBatchEmailAttachmentIngestionErrorType.INVALID_NUMBER,
    CrimeBatchEmailAttachmentIngestionErrorType.MISSING_BATCH_ID,
    CrimeBatchEmailAttachmentIngestionErrorType.MULTIPLE_LOCATION_DATA_TYPES,
    CrimeBatchEmailAttachmentIngestionErrorType.CRIME_DATE_TIME_EXCEEDS_WINDOW,
    ->
      "Amend formatting issues"
    else ->
      if (field != null) "Provide the missing field value" else "Review and amend the record"
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
    }
  }
}
