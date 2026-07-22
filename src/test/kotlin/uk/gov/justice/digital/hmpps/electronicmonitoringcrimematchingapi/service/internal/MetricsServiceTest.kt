package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.EmailData
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.EmailIngestionOutcome
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.IngestionStatus

@ActiveProfiles("test")
class MetricsServiceTest {
  private lateinit var service: MetricsService
  private lateinit var meterRegistry: MeterRegistry

  @BeforeEach
  fun setup() {
    meterRegistry = SimpleMeterRegistry()
    service = MetricsService(meterRegistry)
  }

  @Test
  fun `it should register and increment ingestion outcome counter metric`() {
    val emailData = EmailData(
      sender = "sender",
      originalSender = "originalSender",
      subject = "subject",
      sentAt = java.util.Date(),
      attachments = emptyList(),
    )

    val ingestionOutcome = EmailIngestionOutcome(
      policeForce = "Bedfordshire",
      ingestionStatus = IngestionStatus.SUCCESSFUL,
      emailData = emailData,
      records = emptyList(),
      recordCount = 0,
    )

    service.recordOutcome(ingestionOutcome)

    val outcomeMetric = meterRegistry.get("email.ingestion.outcome").tags(
      "policeForce",
      "Bedfordshire",
      "ingestionStatus",
      IngestionStatus.SUCCESSFUL.name,
    ).counter().count()

    assertThat(outcomeMetric).isEqualTo(1.0)
  }
}
