package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena

data class AthenaDeviceActivationDto(
  val deviceActivationId: Long,
  val deviceId: Long,
  val deviceActivationDate: String,
  val deviceDeactivationDate: String?,
)
