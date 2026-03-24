package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config.notify.NotifyProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeRecordRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.EmailIngestionOutcome
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeBatchEmailIngestionErrorType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.IngestionStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.validation.EmailAttachmentIngestionError
import uk.gov.service.notify.NotificationClient
import java.time.LocalDate

@Service
class EmailNotificationService(
  private val notifyClient: NotificationClient,
  private val properties: NotifyProperties,
) {
  fun sendEmails(
    ingestionOutcome: EmailIngestionOutcome,
  ) {
    val templateId = emailTemplateId(ingestionOutcome.ingestionStatus)

    val personalisation = buildPersonalisation(
      status = ingestionOutcome.ingestionStatus,
      fileName = ingestionOutcome.emailData.attachments.firstOrNull()?.name ?: "Invalid File",
      batchId = ingestionOutcome.batchId,
      policeForce = ingestionOutcome.policeForce,
      errorType = ingestionOutcome.errorType,
      records = ingestionOutcome.records,
      errors = ingestionOutcome.errors,
      recordCount = ingestionOutcome.recordCount,
    )

    val emailAddresses = listOf(
      ingestionOutcome.emailData.sender,
      ingestionOutcome.emailData.originalSender,
    )

    for (emailAddress in emailAddresses) {
      sendEmail(
        templateId = templateId,
        emailAddress = emailAddress,
        personalisation = personalisation,
        reference = ingestionOutcome.batchId,
      )
    }
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

  private fun emailTemplateId(
    status: IngestionStatus,
  ): String = when (status) {
    IngestionStatus.FAILED -> properties.failedIngestionTemplateId
    IngestionStatus.SUCCESSFUL -> properties.successfulIngestionTemplateId
    IngestionStatus.PARTIAL -> properties.partialIngestionTemplateId
    IngestionStatus.ERROR -> properties.errorIngestionTemplateId
    IngestionStatus.UNKNOWN -> properties.failedIngestionTemplateId
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
        "${error.crimeReference ?: ""},Failed, ${error.errorType.message},${error.errorType.requiredAction}",
      )
    }
  }.toByteArray(Charsets.UTF_8)

  private fun buildPersonalisation(
    status: IngestionStatus,
    fileName: String,
    batchId: String,
    policeForce: String,
    errorType: CrimeBatchEmailIngestionErrorType,
    errors: List<EmailAttachmentIngestionError>,
    records: List<CrimeRecordRequest>,
    recordCount: Int = 0,
  ): Map<String, Any> {
    val personalisation = hashMapOf<String, Any>()
    personalisation["fileName"] = fileName
    personalisation["ingestionDate"] = LocalDate.now().toString()
    personalisation["batchId"] = batchId
    personalisation["policeForce"] = policeForce

    when (status) {
      IngestionStatus.SUCCESSFUL -> {
        personalisation["linkToFile"] = NotificationClient.prepareUpload(records.toCsv().toByteArray(), fileName)
      }
      IngestionStatus.FAILED -> {
        personalisation["errorSummary"] = errorType.message
        personalisation["totalCount"] = recordCount
      }
      IngestionStatus.PARTIAL, IngestionStatus.ERROR -> {
        personalisation["errorSummary"] = buildInLineErrorSummary(errors)
        personalisation["totalCount"] = recordCount
        personalisation["successCount"] = records.size
        personalisation["failedCount"] = recordCount - records.size
        personalisation["linkToFile"] = NotificationClient.prepareUpload(buildErrorCsv(errors), "ingestion_errors.csv")
      }
      IngestionStatus.UNKNOWN -> {}
    }

    return personalisation
  }
}
