package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class CrimeDetailsTest {
  @ParameterizedTest
  @CsvSource(
    "batchId",
    "reference",
    "type",
  )
  fun `it should reject blank required fields`(field: String) {
    val valid = CrimeDetails(
      batchId = "AVS20260101",
      reference = "123456789",
      type = "Robbery",
    )

    assertThatThrownBy {
      when (field) {
        "batchId" -> valid.copy(batchId = " ")
        "reference" -> valid.copy(reference = " ")
        "type" -> valid.copy(type = " ")
        else -> error("Unknown field")
      }
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("'$field' cannot be blank")
  }
}
