package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config.notify.NotifyProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeRecordRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.EmailData
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmailAttachmentIngestionError
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeBatchEmailAttachmentIngestionErrorType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeBatchEmailIngestionErrorType
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
    rowErrors: List<CrimeBatchEmailAttachmentIngestionError> = emptyList(),
    errorType: CrimeBatchEmailIngestionErrorType? = null,
  ) {
    val emailAddresses = listOf(emailData.sender, emailData.originalSender)
    val policeForceArea = emailData.sender.substringAfter("@").substringBefore(".")

    val errorSummary = when {
      rowErrors.isNotEmpty() -> buildTop5ErrorSummary(rowErrors)
      errorType != null -> errorType.message
      else -> "Unknown error"
    }
    
    val personalisation = hashMapOf<String, Any>()
    
    personalisation["batchId"] = "Unknown due to error"
    personalisation["policeForce"] = policeForceArea
    personalisation["ingestionDate"] = LocalDate.now().toString()
    personalisation["errorSummary"] = errorSummary
    personalisation["totalCount"] = rowErrors.size
    personalisation["successCount"] = 0
    personalisation["failedCount"] = rowErrors.size

    for (emailAddress in emailAddresses) {
      sendEmail(properties.failedIngestionTemplateId, emailAddress, personalisation, "batchId")
    }
  }

  fun sendPartialIngestionEmail(
    batchId: String,
    policeForce: PoliceForce,
    emailData: EmailData,
    records: List<CrimeRecordRequest>,
    errors: List<CrimeBatchEmailIngestionError>,
    successCount: Int,
  ) {
    val emailAddresses = listOf(emailData.sender, emailData.originalSender)
    val errorSummary = "Table"
    val totalCount = Int

    
    val personalisation = hashMapOf<String, Any>()
    personalisation["fileName"] = emailData.attachments.single().name
    personalisation["ingestionDate"] = LocalDate.now().toString()
    personalisation["batchId"] = batchId
    personalisation["policeForce"] = policeForce.name
    personalisation[errorSummary] = buildTop5ErrorSummary(errors)
    personalisation[totalCount] = successCount + errors.size
    personalisation[successCount] = successCount
    personalisation[failedCount] = errors.size

    if (errors.size > 5) {
      personalisation["linkToErrorFile"] = NotificationClient.prepareUpload(buildErrorCsv, false)
    }

    for (emailAddress in emailAddresses) {
      sendEmail(properties.partialIngestionTemplateId, emailAddress, personalisation, batchId)
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

  private fun buildTop5ErrorSummary(errors: List<CrimeBatchEmailIngestionError>): String =
    errors.take(5).joinToString(separator = "\n") { error ->
      buildString {
        append("Row ${error.rowNumber}: [${error.errorType}] ${error.errorType.message}")
        if (error.fieldName != null) append(" (field: ${error.fieldName})")
        if (error.value != null) append(" (value: ${error.value})")
      }
    }
  
  private fun buildErrorCsv(errors: List<CrimeBatchEmailIngestionError>): ByteArray = 
    buildString {
      appendLine("Row Number,Crime Reference,Error Type,Field Name,Value")
      errors.forEach { error ->
        appendLine("${error.rowNumber},${error.crimeReference ?: ""},${error.errorType},${error.fieldName ?: ""},${error.value ?: ""}")
      }
    }.toByteArray(Charsets.UTF_8)

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
