package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

data class DeviceWearerPositionResponse(
  val capturedDateTime: String,
  val direction: Long,
  val entryBearing: Double?,
  val exitBearing: Double?,
  val latitude: Double,
  val longitude: Double,
  val precision: Long,
  val sequenceLabel: String,
  val speed: Long,
)
