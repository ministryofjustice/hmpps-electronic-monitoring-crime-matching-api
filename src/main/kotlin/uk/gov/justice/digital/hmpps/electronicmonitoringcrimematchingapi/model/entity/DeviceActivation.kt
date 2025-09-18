package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity

import java.time.LocalDateTime

data class DeviceActivation(
  val deviceActivationId: Long,
  val deviceId: Long,
  val deviceName: String,
  val personId: Long,
  val deviceActivationDate: LocalDateTime?,
  val deviceDeactivationDate: LocalDateTime?,
  val orderStart: String,
  val orderEnd: String,
)
