package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.parser

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.ContextualReportRequestParameters
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentType
import java.time.Instant

class DocumentRequestParameterParserTest {
  private val parser = DocumentRequestParameterParser()

  private val validParameters = mapOf(
    "deviceId" to 123456,
    "from" to "2026-10-01T00:00:00Z",
    "to" to "2026-10-02T00:00:00Z",
  )

  @Test
  fun `it should parse contextual report parameters`() {
    val result = parser.parse(
      DocumentType.CONTEXTUAL_REPORT,
      validParameters,
    )

    assertThat(result).isEqualTo(
      ContextualReportRequestParameters(
        deviceId = 123456,
        from = Instant.parse("2026-10-01T00:00:00Z"),
        to = Instant.parse("2026-10-02T00:00:00Z"),
      ),
    )
  }

  @Test
  fun `it should reject missing device ID`() {
    assertThatThrownBy {
      parser.parse(
        DocumentType.CONTEXTUAL_REPORT,
        validParameters - "deviceId",
      )
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Missing or invalid 'deviceId' parameter")
  }

  @Test
  fun `it should reject invalid device ID type`() {
    assertThatThrownBy {
      parser.parse(
        DocumentType.CONTEXTUAL_REPORT,
        validParameters + ("deviceId" to "invalid"),
      )
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Missing or invalid 'deviceId' parameter")
  }

  @Test
  fun `it should reject missing from timestamp`() {
    assertThatThrownBy {
      parser.parse(
        DocumentType.CONTEXTUAL_REPORT,
        validParameters - "from",
      )
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Missing or invalid 'from' parameter")
  }

  @Test
  fun `it should reject missing to timestamp`() {
    assertThatThrownBy {
      parser.parse(
        DocumentType.CONTEXTUAL_REPORT,
        validParameters - "to",
      )
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Missing or invalid 'to' parameter")
  }

  @Test
  fun `it should reject invalid date range`() {
    assertThatThrownBy {
      parser.parse(
        DocumentType.CONTEXTUAL_REPORT,
        validParameters + ("to" to "2026-09-30T00:00:00Z"),
      )
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("'from' must be before 'to'")
  }
}
