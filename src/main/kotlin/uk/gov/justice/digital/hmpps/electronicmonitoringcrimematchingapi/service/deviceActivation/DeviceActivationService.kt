package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.deviceActivation

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.deviceactivation.DeviceActivation
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.deviceActivation.DeviceActivationRepository

@Service
class DeviceActivationService(
  val deviceActivationRepository: DeviceActivationRepository,
) {
  fun getDeviceActivation(id: Long): DeviceActivation = this.deviceActivationRepository
    .getDeviceActivationById(id)
    .orElseThrow {
      EntityNotFoundException("No device activation found with id: $id")
    }
}
