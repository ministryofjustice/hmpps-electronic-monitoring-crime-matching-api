package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config.notify.NotifyProperties
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
  ) {
    val emailAddresses = listOf(emailData.sender, emailData.originalSender)

    val personalisation = hashMapOf<String, String>()
    personalisation.put("fileName", emailData.attachment.name)
    personalisation.put("ingestionDate", LocalDate.now().toString())
    personalisation.put("batchId", batchId)
    personalisation.put("policeForce", policeForce.name)

    for (emailAddress in emailAddresses) {
      sendEmail(properties.successfulIngestionTemplateId, emailAddress, personalisation, batchId)
    }
  }

  private fun sendEmail(
    templateId: String,
    emailAddress: String,
    personalisation: Map<String, String>,
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
