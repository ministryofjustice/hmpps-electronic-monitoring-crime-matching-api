package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.testutils.DocumentRequestParametersFixtures.givenContextualReportRequestParameters

class ReportRequestDetailsTest {
  private val valid = givenContextualReportRequestParameters().reportRequest

  @ParameterizedTest
  @ValueSource(strings = ["policeForceArea", "requestedBy"])
  fun `it should reject blank required fields`(field: String) {
    assertThatThrownBy {
      when (field) {
        "policeForceArea" -> valid.copy(policeForceArea = " ")
        "requestedBy" -> valid.copy(requestedBy = " ")
        else -> error("Unknown field: $field")
      }
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("'$field' cannot be blank")
  }
}
