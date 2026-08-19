package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal

import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.EmailSendMessage
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.EmailOutboxStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.outbox.EmailOutboxPayloadMapper
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.outbox.EmailOutboxService
import uk.gov.service.notify.NotificationClientException

/**
 * Idempotently sends the confirmation/failure email for an outbox event.
 *
 * - Terminal rows (SENT/FAILED/DEAD) are no-ops, so SQS redelivery cannot duplicate an email.
 * - GOV.UK Notify is called with `reference = event_id` as a second dedupe layer.
 * - A permanent GOV.UK Notify failure (4xx other than 429) is marked FAILED and not retried,
 *   to avoid a retry storm on a non-recoverable error.
 * - Any other failure is re-thrown so SQS redelivers and, after `maxReceiveCount`, moves it to
 *   the DLQ for manual review (mirroring the ingestion DLQ process).
 */
@Service
class EmailSendListener(
  private val emailOutboxService: EmailOutboxService,
  private val emailOutboxPayloadMapper: EmailOutboxPayloadMapper,
  private val emailNotificationService: EmailNotificationService,
  @Value("\${email.outbox.max-receive-count:3}") private val maxReceiveCount: Int,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  companion object {
    private const val TOO_MANY_REQUESTS = 429
  }

  @SqsListener("emailsend", factory = "hmppsQueueContainerFactoryProxy")
  fun receiveEmailSend(
    message: EmailSendMessage,
    @Header(value = "ApproximateReceiveCount", required = false) receiveCount: String?,
  ) {
    val attempt = receiveCount?.toIntOrNull() ?: 1
    val row = emailOutboxService.find(message.eventId)
    if (row == null) {
      log.warn("Received emailsend message for unknown outbox event {}", message.eventId)
      return
    }

    if (row.status == EmailOutboxStatus.SENT ||
      row.status == EmailOutboxStatus.FAILED ||
      row.status == EmailOutboxStatus.DEAD
    ) {
      log.debug("Skipping terminal ({}) outbox event {}", row.status, row.eventId)
      return
    }

    try {
      val payload = emailOutboxPayloadMapper.readPayload(row.payload)
      val outcome = emailOutboxPayloadMapper.toOutcome(payload)
      emailNotificationService.sendEmail(outcome, payload.recipient, row.eventId.toString())
      emailOutboxService.markSent(row.eventId)
      log.info("Email outbox event {} sent successfully on attempt {}", row.eventId, attempt)
    } catch (e: Exception) {
      val permanentStatus = permanentFailureStatus(e)
      if (permanentStatus != null) {
        log.error(
          "Email outbox event {} failed permanently (HTTP {}, ApproximateReceiveCount='{}'); marking FAILED",
          row.eventId,
          permanentStatus,
          receiveCount,
          e,
        )
        emailOutboxService.markFailed(row.eventId, e.message)
        return
      }

      if (attempt >= maxReceiveCount) {
        log.error(
          "Email outbox event {} failed on final attempt {} of {} (ApproximateReceiveCount='{}'); marking DEAD",
          row.eventId,
          attempt,
          maxReceiveCount,
          receiveCount,
          e,
        )
        emailOutboxService.markDead(row.eventId, e.message)
      } else {
        log.warn(
          "Email outbox event {} failed on attempt {} of {} (ApproximateReceiveCount='{}'); will retry",
          row.eventId,
          attempt,
          maxReceiveCount,
          receiveCount,
          e,
        )
        emailOutboxService.markRetry(row.eventId, e.message)
      }
      throw e
    }
  }

  /**
   * Returns the HTTP status if [e] (or a cause) is a GOV.UK Notify permanent failure
   * (a 4xx other than 429 Too Many Requests), otherwise null (treat as transient/retryable).
   */
  private fun permanentFailureStatus(e: Throwable): Int? {
    var current: Throwable? = e
    while (current != null) {
      if (current is NotificationClientException) {
        val status = current.httpResult
        return if (status in 400..499 && status != TOO_MANY_REQUESTS) status else null
      }
      current = current.cause
    }
    return null
  }
}
