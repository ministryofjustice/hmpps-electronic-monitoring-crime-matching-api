package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.deviceActivation

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.DeviceActivation
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.GeolocationMechanism
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.Position
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.deviceActivation.DeviceActivationRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.position.PositionRepository
import java.time.ZonedDateTime

@Service
class DeviceActivationService(
  val deviceActivationRepository: DeviceActivationRepository,
  val positionRepository: PositionRepository,
) {
  fun getDeviceActivation(id: Long): DeviceActivation = this.deviceActivationRepository
    .findById(id)
    .orElseThrow {
      EntityNotFoundException("No device activation found with id: $id")
    }

  fun getDeviceActivationPositions(
    id: Long,
    geolocationMechanism: GeolocationMechanism?,
    from: ZonedDateTime?,
    to: ZonedDateTime?,
  ): List<Position> = this.positionRepository
    .findByDeviceActivationId(id, geolocationMechanism, from, to)
}
