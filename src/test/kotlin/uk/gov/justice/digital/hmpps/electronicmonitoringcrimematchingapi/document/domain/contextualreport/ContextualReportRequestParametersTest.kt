package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.testutils.DocumentRequestParametersFixtures
import java.time.Instant

class ContextualReportRequestParametersTest {
  @Test
  fun `it should create valid contextual report parameters`() {
    val parameters = DocumentRequestParametersFixtures.givenContextualReportRequestParameters()

    Assertions.assertThat(parameters.type).isEqualTo(DocumentType.CONTEXTUAL_REPORT)
    Assertions.assertThat(parameters.positionSegments).isNotEmpty()
  }

  @Test
  fun `it should reject empty position segments`() {
    Assertions.assertThatThrownBy {
      DocumentRequestParametersFixtures.givenContextualReportRequestParameters().copy(
        positionSegments = emptyList(),
      )
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("'positionSegments' must not be empty")
  }

  @Test
  fun `it should reject a report period ending after the request date`() {
    val parameters = DocumentRequestParametersFixtures.givenContextualReportRequestParameters()

    Assertions.assertThatThrownBy {
      parameters.copy(
        reportPeriod = ReportPeriod(
          start = Instant.parse("2026-10-03T00:00:00Z"),
          end = Instant.parse("2026-10-04T00:00:00Z"),
        ),
      )
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage(
        "'reportPeriod.end' must be before 'reportRequest.requestedAt'",
      )
  }
}
