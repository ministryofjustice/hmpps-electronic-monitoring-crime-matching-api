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
        device_activations.device_activation_id, 
        device_activations.device_id, 
        device_activations.person_id, 
        device_activations.device_activation_date, 
        device_activations.device_deactivation_date 
      FROM 
        device_activations
      WHERE 
        device_activations.device_activation_id = ?
      """.trimIndent().replace("\\s+".toRegex(), " "),
    )
    assertThat(query.parameters).isEqualTo(listOf("0"))
  }
}
