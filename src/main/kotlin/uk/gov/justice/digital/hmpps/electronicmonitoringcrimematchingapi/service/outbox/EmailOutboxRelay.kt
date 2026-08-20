package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.outbox

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * Polls the email outbox and dispatches PENDING events to the `emailsend` queue.
 *
 * Multi-replica safe: [EmailOutboxService.claimBatch] uses `FOR UPDATE SKIP LOCKED`.
 * [dispatchPending] is public so tests can trigger a cycle deterministically instead of
 * waiting on the scheduler.
 */
@Component
class EmailOutboxRelay(
  private val emailOutboxService: EmailOutboxService,
  private val emailSendQueueService: EmailSendQueueService,
  @Value("\${email.outbox.batch-size:50}") private val batchSize: Int,
  @Value("\${email.outbox.lease-timeout-ms:120000}") private val leaseTimeoutMs: Long,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  fun dispatchPending() {
    emailOutboxService.reclaimExpired(Duration.ofMillis(leaseTimeoutMs))

    val claimed = emailOutboxService.claimBatch(batchSize)
    if (claimed.isEmpty()) return

    log.debug("Dispatching {} email outbox event(s)", claimed.size)
    claimed.forEach { row ->
      try {
        emailSendQueueService.publish(row.eventId)
      } catch (e: Exception) {
        // Row stays CLAIMED and is reclaimed to PENDING after the lease times out.
        log.error("Failed to publish email outbox event {} to the emailsend queue", row.eventId, e)
      }
    }
  }
}
