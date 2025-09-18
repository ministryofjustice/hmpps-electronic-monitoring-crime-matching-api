package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

data class DeviceActivationDto(
  val deviceActivationId: Long,
  val deviceId: Long,
  val deviceName: String,
  val personId: Long,
  val deviceActivationDate: String,
  val deviceDeactivationDate: String?,
  val orderStart: String,
  val orderEnd: String,
)
