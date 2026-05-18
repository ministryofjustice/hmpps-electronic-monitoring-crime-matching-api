package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.deviceActivation

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.AthenaQuery
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.DeviceActivation

class GetDeviceActivationByIdQueryBuilder(private val id: Long) {
  fun build(): AthenaQuery = DeviceActivation
    .select(
      DeviceActivation.deviceActivationId,
      DeviceActivation.deviceId,
      DeviceActivation.personId,
      DeviceActivation.deviceActivationDate,
      DeviceActivation.deviceDeactivationDate,
    )
    .where {
      DeviceActivation.deviceActivationId eq id
    }
    .prepare()
}
