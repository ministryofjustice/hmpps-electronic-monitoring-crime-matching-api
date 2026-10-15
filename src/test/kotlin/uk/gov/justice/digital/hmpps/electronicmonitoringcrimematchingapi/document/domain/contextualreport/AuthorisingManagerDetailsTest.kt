package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.testutils.DocumentRequestParametersFixtures.givenContextualReportRequestParameters

class AuthorisingManagerDetailsTest {
  private val valid = givenContextualReportRequestParameters().authorisingManager

  @ParameterizedTest
  @ValueSource(strings = ["address", "name", "occupation"])
  fun `it should reject blank required fields`(field: String) {
    assertThatThrownBy {
      when (field) {
        "address" -> valid.copy(address = " ")
        "name" -> valid.copy(name = " ")
        "occupation" -> valid.copy(occupation = " ")
        else -> error("Unknown field: $field")
      }
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("'$field' cannot be blank")
  }
}
