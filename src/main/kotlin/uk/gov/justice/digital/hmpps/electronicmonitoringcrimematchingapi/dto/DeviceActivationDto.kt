package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

import java.time.LocalDateTime

data class DeviceActivationDto(
  val deviceActivationId: Int,
  val deviceId: Int,
  val deviceName: String?,
  val personId: Int,
  val deviceActivationDate: LocalDateTime?,
  val deviceDeactivationDate: LocalDateTime?,
  val orderStart: String?,
  val orderEnd: String?,
)
