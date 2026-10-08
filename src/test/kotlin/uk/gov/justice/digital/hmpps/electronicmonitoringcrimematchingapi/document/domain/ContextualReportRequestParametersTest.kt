package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant

class ContextualReportRequestParametersTest {
  private val from = Instant.parse("2026-10-01T00:00:00Z")
  private val to = Instant.parse("2026-10-02T00:00:00Z")

  @Test
  fun `it should create valid contextual report parameters`() {
    val parameters = ContextualReportRequestParameters(
      deviceId = 123456,
      from = from,
      to = to,
    )

    assertThat(parameters.deviceId).isEqualTo(123456)
    assertThat(parameters.from).isEqualTo(from)
    assertThat(parameters.to).isEqualTo(to)
    assertThat(parameters.type).isEqualTo(DocumentType.CONTEXTUAL_REPORT)
  }

  @Test
  fun `it should reject non-positive device ID`() {
    assertThatThrownBy {
      ContextualReportRequestParameters(
        deviceId = 0,
        from = from,
        to = to,
      )
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("'deviceId' must be a positive integer")
  }

  @Test
  fun `it should reject end time before start time`() {
    assertThatThrownBy {
      ContextualReportRequestParameters(
        deviceId = 123456,
        from = to,
        to = from,
      )
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("'from' must be before 'to'")
  }

  @Test
  fun `it should reject identical start and end times`() {
    assertThatThrownBy {
      ContextualReportRequestParameters(
        deviceId = 123456,
        from = from,
        to = from,
      )
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("'from' must be before 'to'")
  }
}
