package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.deviceActivation

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config.datastore.DatastoreProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.SqlQueryBuilder

class GetDeviceActivationByIdQueryBuilder : SqlQueryBuilder {
  constructor(
    datastoreProperties: DatastoreProperties,
    id: Long,
  ) : super("${datastoreProperties.mdssDatabase}.device_activation") {
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
