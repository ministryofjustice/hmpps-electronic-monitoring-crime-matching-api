package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.deviceactivation

import java.time.LocalDateTime

data class DeviceActivation(
  val deviceActivationId: Long,
  val deviceId: Long,
  val deviceActivationDate: LocalDateTime,
  val deviceDeactivationDate: LocalDateTime?,
)
