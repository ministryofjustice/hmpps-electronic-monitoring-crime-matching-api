package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.deviceactivation

import java.time.LocalDateTime

data class DeviceActivationDto(
  val deviceActivationId: String,
  val deviceId: String,
  val deviceActivationDate: LocalDateTime?,
  val deviceDeactivationDate: LocalDateTime?,
)
