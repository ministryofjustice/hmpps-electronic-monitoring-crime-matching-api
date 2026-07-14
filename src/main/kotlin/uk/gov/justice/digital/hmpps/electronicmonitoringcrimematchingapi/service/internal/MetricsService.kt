package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.EmailIngestionOutcome

@Service
class MetricsService(private val meterRegistry: MeterRegistry) {

  companion object {
    private const val MESSAGES_RECEIVED_TOTAL = "email.ingestion.received"
    private const val MESSAGE_OUTCOME = "email.ingestion.outcome"
  }

  private val receivedCounter = Counter.builder(MESSAGES_RECEIVED_TOTAL)
    .description("Total number of email ingestion messages received")
    .register(meterRegistry)

  fun recordReceived() {
    receivedCounter.increment()
  }

  fun recordOutcome(outcome: EmailIngestionOutcome) {
    Counter.builder(MESSAGE_OUTCOME)
      .description("Email ingestion outcomes by police force and status")
      .tag("policeForce", outcome.policeForce)
      .tag("ingestionStatus", outcome.ingestionStatus.name)
      .register(meterRegistry)
      .increment()
  }
}
