package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

data class AthenaDeviceActivationDto(
  val deviceActivationId: Long,
  val deviceId: Long,
  val personId: Long,
  val deviceActivationDate: String,
  val deviceDeactivationDate: String?,
)
