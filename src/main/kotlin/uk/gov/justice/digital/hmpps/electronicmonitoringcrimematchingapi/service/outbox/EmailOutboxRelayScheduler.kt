package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.outbox

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Schedules relay cycles in production while allowing tests to disable background dispatch and
 * drive [EmailOutboxRelay.dispatchPending] manually for deterministic assertions.
 */
@Component
@ConditionalOnProperty(
  name = ["email.outbox.relay.scheduling-enabled"],
  havingValue = "true",
  matchIfMissing = true,
)
class EmailOutboxRelayScheduler(
  private val emailOutboxRelay: EmailOutboxRelay,
) {
  @Scheduled(fixedDelayString = "\${email.outbox.relay.interval-ms:5000}")
  fun dispatchPending() {
    emailOutboxRelay.dispatchPending()
  }
}
