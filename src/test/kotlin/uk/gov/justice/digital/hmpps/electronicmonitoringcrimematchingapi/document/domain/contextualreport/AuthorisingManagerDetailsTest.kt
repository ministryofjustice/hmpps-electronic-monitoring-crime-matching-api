package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.testutils.DocumentRequestParametersFixtures.givenContextualReportRequestParameters

class AuthorisingManagerDetailsTest {
  private val valid = givenContextualReportRequestParameters().authorisingManager

  @ParameterizedTest
  @ValueSource(strings = ["name", "occupation"])
  fun `it should reject blank required fields`(field: String) {
    assertThatThrownBy {
      when (field) {
        "name" -> valid.copy(name = " ")
        "occupation" -> valid.copy(occupation = " ")
        else -> error("Unknown field: $field")
      }
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("'$field' cannot be blank")
  }

  @Test
  fun `it should compare signature images by content`() {
    val first = valid.copy(signatureImage = byteArrayOf(1, 2, 3))
    val second = valid.copy(signatureImage = byteArrayOf(1, 2, 3))

    assertThat(first).isEqualTo(second)
    assertThat(first.hashCode()).isEqualTo(second.hashCode())
    assertThat(first).isNotEqualTo(valid.copy(signatureImage = byteArrayOf(4)))
    assertThat(first).isNotEqualTo(valid)
  }
}
