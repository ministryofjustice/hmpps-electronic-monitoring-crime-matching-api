package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.deviceActivation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config.datastore.DatastoreProperties

class GetDeviceActivationByIdQueryBuilderTest {
  private val dataStoreProperties = DatastoreProperties(
    fmsDatabase = "",
    mdssDatabase = "allied_mdss_test",
    outputBucketArn = "",
    retryIntervalMs = 0,
    workgroup = "",
  )

  @Test
  fun `it should build a valid query`() {
    val id: Long = 0
    val query = GetDeviceActivationByIdQueryBuilder(
      dataStoreProperties,
      id,
    ).build()

    assertThat(query.queryString).isEqualTo("SELECT device_activation_id, device_id, person_id, device_activation_date, device_deactivation_date FROM allied_mdss_test.device_activation t WHERE device_activation_id = ?")
    assertThat(query.parameters).isEqualTo(listOf(id.toString()))
  }
}
