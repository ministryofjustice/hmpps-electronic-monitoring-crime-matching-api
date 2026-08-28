package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.outbox

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.EmailIngestionOutcome
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.EmailOutbox
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.EmailOutboxStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.outbox.EmailOutboxRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal.FeatureFlagService
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal.MetricsService
import java.net.InetAddress
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

@Service
class EmailOutboxService(
  private val emailOutboxRepository: EmailOutboxRepository,
  private val emailOutboxPayloadMapper: EmailOutboxPayloadMapper,
  private val featureFlagService: FeatureFlagService,
  private val metricsService: MetricsService,
  private val clock: Clock,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  private val instanceId: String = runCatching { InetAddress.getLocalHost().hostName }
    .getOrDefault("unknown-${UUID.randomUUID()}")

  /**
   * Records the intent to send an email for a committed ingestion outcome. A separate durable
   * PENDING row is written per recipient so the relay/worker can deliver each recipient exactly
   * once with independent retries (a partial failure never re-sends an already-delivered recipient).
   */
  @Transactional
  fun enqueue(outcome: EmailIngestionOutcome): List<EmailOutbox> {
    val now = now()
    val crimeBatchId = outcome.crimeBatchId.toUuidOrNull()
    return resolveRecipients(outcome).map { recipient ->
      val row = EmailOutbox(
        crimeBatchId = crimeBatchId,
        status = EmailOutboxStatus.PENDING,
        payload = emailOutboxPayloadMapper.toJson(outcome, recipient),
        availableAt = now,
        createdAt = now,
        updatedAt = now,
      )
      val saved = emailOutboxRepository.save(row)
      metricsService.recordOutboxEvent(EmailOutboxStatus.PENDING)
      log.debug("Enqueued email outbox event {} for crime batch {}", saved.eventId, saved.crimeBatchId)
      saved
    }
  }

  /**
   * Atomically leases a batch of eligible PENDING rows (see [EmailOutboxRepository.claimBatch])
   * and marks them CLAIMED. Safe to run concurrently across replicas.
   */
  @Transactional
  fun claimBatch(limit: Int): List<EmailOutbox> {
    val now = now()
    val claimed = emailOutboxRepository.claimBatch(now, limit)
    claimed.forEach { row ->
      row.status = EmailOutboxStatus.CLAIMED
      row.claimedAt = now
      row.claimedBy = instanceId
      row.updatedAt = now
      metricsService.recordOutboxEvent(EmailOutboxStatus.CLAIMED)
    }
    return claimed
  }

  /** Returns CLAIMED rows leased longer ago than [leaseTimeout] back to PENDING. */
  @Transactional
  fun reclaimExpired(leaseTimeout: Duration): Int {
    val now = now()
    val reclaimed = emailOutboxRepository.reclaimExpired(now, now.minus(leaseTimeout))
    if (reclaimed > 0) {
      log.warn("Reclaimed {} stale CLAIMED email outbox event(s) back to PENDING", reclaimed)
    }
    return reclaimed
  }

  @Transactional(readOnly = true)
  fun find(eventId: UUID): EmailOutbox? = emailOutboxRepository.findById(eventId).orElse(null)

  @Transactional
  fun markSent(eventId: UUID) {
    updateFromClaimed(eventId, EmailOutboxStatus.SENT, null)
  }

  @Transactional
  fun markRetry(eventId: UUID, error: String?) {
    updateFromClaimed(eventId, EmailOutboxStatus.CLAIMED, error)
  }

  /** Permanent, non-retryable failure (e.g. a GOV.UK Notify 4xx). No further delivery is attempted. */
  @Transactional
  fun markFailed(eventId: UUID, error: String?) {
    updateFromClaimed(eventId, EmailOutboxStatus.FAILED, error)
  }

  @Transactional
  fun markDead(eventId: UUID, error: String?) {
    updateFromClaimed(eventId, EmailOutboxStatus.DEAD, error)
  }

  private fun updateFromClaimed(eventId: UUID, status: EmailOutboxStatus, error: String?) {
    val updated = emailOutboxRepository.transitionStatusIfCurrent(
      eventId = eventId,
      expectedStatus = EmailOutboxStatus.CLAIMED.name,
      newStatus = status.name,
      lastError = error,
      updatedAt = now(),
    )
    if (updated == 0) {
      val currentStatus = emailOutboxRepository.findById(eventId).orElse(null)?.status
      if (currentStatus == null) {
        log.debug("Skipping transition of unknown email outbox event {} to {}", eventId, status)
      } else {
        log.debug(
          "Skipping stale email outbox transition for event {} from {} to {}; current status is {}",
          eventId,
          EmailOutboxStatus.CLAIMED,
          status,
          currentStatus,
        )
      }
      return
    }

    metricsService.recordOutboxEvent(status)
  }

  private fun now(): LocalDateTime = LocalDateTime.now(clock)

  private fun String.toUuidOrNull(): UUID? = runCatching { UUID.fromString(this) }.getOrNull()

  /**
   * The recipients an ingestion outcome should be emailed to: always the forwarding sender,
   * and additionally the original police sender when the police-confirmation feature flag is on.
   */
  private fun resolveRecipients(
    ingestionOutcome: EmailIngestionOutcome,
  ): List<String> = buildList {
    add(ingestionOutcome.emailData.sender)
    if (featureFlagService.policeConfirmationEmailsEnabled()) {
      add(ingestionOutcome.emailData.originalSender)
    }
  }
}
