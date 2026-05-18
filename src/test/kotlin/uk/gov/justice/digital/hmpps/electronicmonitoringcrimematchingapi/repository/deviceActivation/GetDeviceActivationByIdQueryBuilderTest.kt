package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.deviceActivation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GetDeviceActivationByIdQueryBuilderTest {
  @Test
  fun `it should build a valid query`() {
    val id: Long = 0
    val query = GetDeviceActivationByIdQueryBuilder(
      id,
    ).build()

    assertThat(query.queryString).isEqualTo(
      """
      SELECT 
        device_activation.device_activation_id, 
        device_activation.device_id, 
        device_activation.person_id, 
        device_activation.device_activation_date, 
        device_activation.device_deactivation_date 
      FROM 
        device_activation 
      WHERE 
        device_activation.device_activation_id = ?
      """.trimIndent().replace("\n", "").replace("   ", " "),
    )
    assertThat(query.parameters).isEqualTo(listOf("0"))
  }
}
