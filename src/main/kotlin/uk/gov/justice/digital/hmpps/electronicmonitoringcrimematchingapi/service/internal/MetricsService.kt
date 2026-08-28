package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.EmailIngestionOutcome
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.EmailOutboxStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.IngestionStatus

@Service
class MetricsService(
  private val meterRegistry: MeterRegistry,
) {
  private val counters =
    mutableMapOf<IngestionStatus, Counter>()

  private val outboxCounters =
    mutableMapOf<EmailOutboxStatus, Counter>()

  companion object {
    private const val MESSAGE_OUTCOME = "email.ingestion.outcome"
    private const val OUTBOX_EVENT = "email.outbox.event"
  }

  @PostConstruct
  fun initialise() {
    IngestionStatus.entries.forEach { status ->
      counters[status] = createEmailIngestionOutcomeCounter(
        ingestionStatus = status,
      )
    }
    EmailOutboxStatus.entries.forEach { status ->
      outboxCounters[status] = createOutboxEventCounter(
        outboxStatus = status,
      )
    }
  }

  private fun createEmailIngestionOutcomeCounter(
    ingestionStatus: IngestionStatus,
  ): Counter = Counter.builder(MESSAGE_OUTCOME)
    .description("Email ingestion outcomes by status")
    .tag("ingestionStatus", ingestionStatus.name)
    .register(meterRegistry)

  private fun createOutboxEventCounter(
    outboxStatus: EmailOutboxStatus,
  ): Counter = Counter.builder(OUTBOX_EVENT)
    .description("Email outbox transitions by status")
    .tag("status", outboxStatus.name)
    .register(meterRegistry)

  fun recordOutcome(outcome: EmailIngestionOutcome) {
    counters
      .getValue(outcome.ingestionStatus)
      .increment()
  }

  fun recordOutboxEvent(status: EmailOutboxStatus) {
    outboxCounters
      .getValue(status)
      .increment()
  }
}
