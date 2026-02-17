package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config.notify.NotifyProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeRecordRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.EmailData
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce
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
    personalisation["fileName"] = emailData.attachment.name
    personalisation["ingestionDate"] = LocalDate.now().toString()
    personalisation["batchId"] = batchId
    personalisation["policeForce"] = policeForce.name
    personalisation["linkToFile"] = NotificationClient.prepareUpload(csvBytes, emailData.attachment.name)

    for (emailAddress in emailAddresses) {
      sendEmail(properties.successfulIngestionTemplateId, emailAddress, personalisation, batchId)
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
    }
  }
}
