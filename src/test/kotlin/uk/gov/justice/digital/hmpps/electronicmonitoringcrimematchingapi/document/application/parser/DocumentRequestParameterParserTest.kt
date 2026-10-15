package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.parser

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.exception.InvalidRequestException
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.testutils.DocumentRequestParametersFixtures.givenContextualReportRequestParameters
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.testutils.DocumentRequestParametersFixtures.givenRawContextualReportRequestParameters

class DocumentRequestParameterParserTest {
  private val objectMapper = JsonMapper.builder()
    .findAndAddModules()
    .build()

  private val parser = DocumentRequestParameterParser(objectMapper)

  private val validParameters = givenRawContextualReportRequestParameters()

  @Test
  fun `it should parse contextual report parameters`() {
    val result = parser.parse(
      DocumentType.CONTEXTUAL_REPORT,
      validParameters,
    )

    assertThat(result).isEqualTo(
      givenContextualReportRequestParameters(),
    )
  }

  @Test
  fun `should reject missing crime details`() {
    val parameters = validParameters - "crime"

    assertThatThrownBy {
      parser.parse(DocumentType.CONTEXTUAL_REPORT, parameters)
    }
      .isInstanceOf(InvalidRequestException::class.java)
      .hasMessage("Invalid parameters for document type CONTEXTUAL_REPORT")
  }

  @Test
  fun `should reject invalid report period`() {
    val parameters = validParameters + (
      "reportPeriod" to mapOf(
        "start" to "2026-10-03T08:00:00Z",
        "end" to "2026-10-02T08:00:00Z",
      )
      )

    assertThatThrownBy {
      parser.parse(DocumentType.CONTEXTUAL_REPORT, parameters)
    }
      .isInstanceOf(InvalidRequestException::class.java)
      .hasMessage("'reportPeriod.start' must be before 'reportPeriod.end'")
  }

  @Test
  fun `should reject empty position segments`() {
    val parameters = validParameters + ("positionSegments" to emptyList<Any>())

    assertThatThrownBy {
      parser.parse(DocumentType.CONTEXTUAL_REPORT, parameters)
    }
      .isInstanceOf(InvalidRequestException::class.java)
      .hasMessage("'positionSegments' must not be empty")
  }
}
