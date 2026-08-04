package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.EmailIngestionOutcome
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.IngestionStatus

@Service
class MetricsService(
  private val meterRegistry: MeterRegistry,
) {
  private val counters =
    mutableMapOf<IngestionStatus, Counter>()

  companion object {
    private const val MESSAGE_OUTCOME = "email.ingestion.outcome"
  }

  @PostConstruct
  fun initialise() {
    IngestionStatus.entries.forEach { status ->
      counters[status] = createEmailIngestionOutcomeCounter(
        ingestionStatus = status,
      )
    }
  }

  private fun createEmailIngestionOutcomeCounter(
    ingestionStatus: IngestionStatus,
  ): Counter = Counter.builder(MESSAGE_OUTCOME)
    .description("Email ingestion outcomes by police force and status")
    .tag("ingestionStatus", ingestionStatus.name)
    .register(meterRegistry)

  fun recordOutcome(outcome: EmailIngestionOutcome) {
    counters
      .getValue(outcome.ingestionStatus)
      .increment()
  }
}
