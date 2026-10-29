package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.Instant

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
      proximityAlertCreatedAt = Instant.parse("2026-09-01T00:00:00Z"),
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
