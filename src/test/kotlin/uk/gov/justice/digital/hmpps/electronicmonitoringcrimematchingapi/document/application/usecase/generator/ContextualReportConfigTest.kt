package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport.ContextualReportConfig
import java.time.Instant

class ContextualReportConfigTest {
  @Test
  fun `it should format dates and times in Europe London`() {
    val config = ContextualReportConfig()

    assertThat(
      config.dateTimeFormatter.format(Instant.parse("2026-01-01T12:00:00Z")),
    ).isEqualTo("01/01/2026 12:00")

    assertThat(
      config.dateTimeFormatter.format(Instant.parse("2026-07-01T12:00:00Z")),
    ).isEqualTo("01/07/2026 13:00")
  }
}
