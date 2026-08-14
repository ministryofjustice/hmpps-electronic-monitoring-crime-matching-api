package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.outbox

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.EmailData
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.NamedDataSource
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.EmailIngestionOutcome
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.EmailOutboxPayload
import java.util.Date

/**
 * Serialises an [EmailIngestionOutcome] (for one recipient) to/from the JSON stored in
 * `email_outbox.payload`, so the email-send worker can rebuild the outcome and send to that
 * single recipient using the existing send path.
 */
@Component
class EmailOutboxPayloadMapper(
  private val objectMapper: ObjectMapper,
) {
  fun toJson(outcome: EmailIngestionOutcome, recipient: String): String = objectMapper.writeValueAsString(toPayload(outcome, recipient))

  fun toPayload(outcome: EmailIngestionOutcome, recipient: String): EmailOutboxPayload = EmailOutboxPayload(
    ingestionStatus = outcome.ingestionStatus,
    recipient = recipient,
    fileName = outcome.emailData.attachments.firstOrNull()?.name ?: "Invalid File",
    batchId = outcome.batchId,
    crimeBatchId = outcome.crimeBatchId,
    policeForce = outcome.policeForce,
    errorType = outcome.errorType,
    recordCount = outcome.recordCount,
    records = outcome.records,
    errors = outcome.errors,
  )

  fun readPayload(json: String): EmailOutboxPayload = objectMapper.readValue(json)

  fun toOutcome(payload: EmailOutboxPayload): EmailIngestionOutcome = EmailIngestionOutcome(
    batchId = payload.batchId,
    crimeBatchId = payload.crimeBatchId,
    policeForce = payload.policeForce,
    errorType = payload.errorType,
    errors = payload.errors,
    emailData = EmailData(
      // The recipient is applied explicitly at send time; these values only feed personalisation.
      sender = payload.recipient,
      originalSender = payload.recipient,
      subject = "",
      sentAt = Date(),
      attachments = listOf(NamedDataSource(payload.fileName)),
    ),
    records = payload.records,
    recordCount = payload.recordCount,
    ingestionStatus = payload.ingestionStatus,
  )
}
