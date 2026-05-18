package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.deviceActivation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.fixtures.queries.AthenaQueries

class GetDeviceActivationByIdQueryBuilderTest {
  @Test
  fun `it should build a valid query`() {
    val id: Long = 0
    val query = GetDeviceActivationByIdQueryBuilder(
      id,
    ).build()

    assertThat(query.queryString).isEqualTo(
      AthenaQueries.SelectDeviceActivationById,
    )
    assertThat(query.parameters).isEqualTo(listOf("0"))
  }
}
