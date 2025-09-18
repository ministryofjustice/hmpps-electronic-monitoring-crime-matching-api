package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.mappers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.AthenaDeviceActivationDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.DeviceActivationDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.formatter
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.nullableLocalDateTime
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.DeviceActivation
import java.time.LocalDateTime

@Component
class DeviceActivationMapper {
  fun fromModelToDto(deviceActivation: DeviceActivation): DeviceActivationDto = with(deviceActivation) {
    DeviceActivationDto(
      deviceActivationId = deviceActivationId,
      deviceId = deviceId,
      deviceName = "",
      personId = personId,
      deviceActivationDate = deviceActivationDate.toString(),
      deviceDeactivationDate = deviceDeactivationDate?.toString(),
      orderStart = "",
      orderEnd = "",
    )
  }

  fun fromAthenaToModel(deviceActivation: AthenaDeviceActivationDto): DeviceActivation = with(deviceActivation) {
    DeviceActivation(
      deviceActivationId = deviceActivationId,
      deviceId = deviceId,
      deviceName = "",
      personId = personId,
      deviceActivationDate = LocalDateTime.parse(deviceActivationDate, formatter),
      deviceDeactivationDate = nullableLocalDateTime(deviceDeactivationDate),
      orderStart = "",
      orderEnd = "",
    )
  }
}
