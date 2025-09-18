package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.deviceActivation

import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.client.EmDatastoreClientInterface
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.AthenaDeviceActivationDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.AthenaHelper
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.mappers.DeviceActivationMapper
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.DeviceActivation
import java.util.Optional

@Repository
class DeviceActivationRepository(
  val athenaClient: EmDatastoreClientInterface,
  val mapper: DeviceActivationMapper,
) {
  fun findById(id: Long): Optional<DeviceActivation> {
    val query = GetDeviceActivationByIdQueryBuilder(id).build()
    val queryResult = athenaClient.getQueryResult(query)
    val deviceActivations = AthenaHelper.Companion.mapTo<AthenaDeviceActivationDto>(queryResult)

    return mapToModel(deviceActivations.firstOrNull())
  }

  private fun mapToModel(athenaDeviceActivation: AthenaDeviceActivationDto?): Optional<DeviceActivation> {
    if (athenaDeviceActivation === null) {
      return Optional.empty()
    }

    return Optional.of(mapper.fromAthenaToModel(athenaDeviceActivation))
  }
}
