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
 * Serialises an [EmailIngestionOutcome] to/from the JSON stored in `email_outbox.payload`,
 * so the email-send worker can rebuild the outcome and reuse the existing send path.
 */
@Component
class EmailOutboxPayloadMapper(
  private val objectMapper: ObjectMapper,
) {
  fun toJson(outcome: EmailIngestionOutcome): String = objectMapper.writeValueAsString(toPayload(outcome))

  fun toPayload(outcome: EmailIngestionOutcome): EmailOutboxPayload = EmailOutboxPayload(
    ingestionStatus = outcome.ingestionStatus,
    sender = outcome.emailData.sender,
    originalSender = outcome.emailData.originalSender,
    fileName = outcome.emailData.attachments.firstOrNull()?.name ?: "Invalid File",
    batchId = outcome.batchId,
    crimeBatchId = outcome.crimeBatchId,
    policeForce = outcome.policeForce,
    errorType = outcome.errorType,
    recordCount = outcome.recordCount,
    records = outcome.records,
    errors = outcome.errors,
  )

  fun toOutcome(json: String): EmailIngestionOutcome {
    val payload = objectMapper.readValue<EmailOutboxPayload>(json)
    return EmailIngestionOutcome(
      batchId = payload.batchId,
      crimeBatchId = payload.crimeBatchId,
      policeForce = payload.policeForce,
      errorType = payload.errorType,
      errors = payload.errors,
      emailData = EmailData(
        sender = payload.sender,
        originalSender = payload.originalSender,
        subject = "",
        sentAt = Date(),
        attachments = listOf(NamedDataSource(payload.fileName)),
      ),
      records = payload.records,
      recordCount = payload.recordCount,
      ingestionStatus = payload.ingestionStatus,
    )
  }
}
