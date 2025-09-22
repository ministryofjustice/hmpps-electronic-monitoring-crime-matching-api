package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.deviceActivation

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.SqlQueryBuilder

class GetDeviceActivationByIdQueryBuilder : SqlQueryBuilder {
  constructor(id: Long) : super("allied_mdss_test_20250714014447.device_activation") {
    this.addFields(
      listOf(
        "device_activation_id",
        "device_id",
        "person_id",
        "device_activation_date",
        "device_deactivation_date",
      ),
    )
      .addFilter("device_activation_id", id)
  }
}
