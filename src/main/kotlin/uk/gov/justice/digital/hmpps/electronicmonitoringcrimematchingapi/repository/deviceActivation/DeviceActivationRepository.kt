package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.deviceActivation

import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.client.EmDatastoreClientInterface
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.AthenaHelper
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.formatter
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.nullableLocalDateTime
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.AthenaDeviceActivationDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.deviceactivation.DeviceActivation
import java.time.LocalDateTime
import java.util.Optional

@Repository
class DeviceActivationRepository(
  val athenaClient: EmDatastoreClientInterface,
) {
  fun getDeviceActivationById(id: Long): Optional<DeviceActivation> {
    val query = GetDeviceActivationByIdQueryBuilder(id).build()
    val queryResult = athenaClient.getQueryResult(query)
    val deviceActivations = AthenaHelper.Companion.mapTo<AthenaDeviceActivationDto>(queryResult)

    return mapTo(deviceActivations.firstOrNull())
  }

  fun mapTo(athenaDeviceActivation: AthenaDeviceActivationDto?): Optional<DeviceActivation> {
    if (athenaDeviceActivation === null) {
      return Optional.empty()
    }

    return Optional.of(
      DeviceActivation(
        deviceActivationId = athenaDeviceActivation.deviceActivationId,
        deviceId = athenaDeviceActivation.deviceId,
        deviceActivationDate = LocalDateTime.parse(athenaDeviceActivation.deviceActivationDate, formatter),
        deviceDeactivationDate = nullableLocalDateTime(athenaDeviceActivation.deviceDeactivationDate),
      ),
    )
  }
}
