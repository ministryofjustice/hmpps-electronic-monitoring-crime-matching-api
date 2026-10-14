package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant

class ReportPeriodTest {
  private val start = Instant.parse("2026-10-01T00:00:00Z")
  private val end = Instant.parse("2026-10-02T00:00:00Z")

  @Test
  fun `it should create a valid report period`() {
    val period = ReportPeriod(start, end)

    assertThat(period.start).isEqualTo(start)
    assertThat(period.end).isEqualTo(end)
  }

  @Test
  fun `it should reject an end before the start`() {
    assertThatThrownBy {
      ReportPeriod(start = end, end = start)
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("'reportPeriod.start' must be before 'reportPeriod.end'")
  }

  @Test
  fun `it should reject identical start and end times`() {
    assertThatThrownBy {
      ReportPeriod(start = start, end = start)
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("'reportPeriod.start' must be before 'reportPeriod.end'")
  }
}
