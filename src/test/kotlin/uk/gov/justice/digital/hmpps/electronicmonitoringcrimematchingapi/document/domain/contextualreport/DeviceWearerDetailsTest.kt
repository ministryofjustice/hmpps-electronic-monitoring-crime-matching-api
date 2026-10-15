package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.testutils.DocumentRequestParametersFixtures.givenContextualReportRequestParameters

class DeviceWearerDetailsTest {
  private val valid = givenContextualReportRequestParameters().deviceWearer

  @ParameterizedTest
  @ValueSource(strings = ["address", "emsId", "name", "offenderManager"])
  fun `it should reject blank required fields`(field: String) {
    assertThatThrownBy {
      when (field) {
        "address" -> valid.copy(address = " ")
        "emsId" -> valid.copy(emsId = " ")
        "name" -> valid.copy(name = " ")
        "offenderManager" -> valid.copy(offenderManager = " ")
        else -> error("Unknown field: $field")
      }
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("'$field' cannot be blank")
  }
}
