package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

data class DeviceWearerPositionResponse(
  val capturedDateTime: String,
  val direction: Int,
  val latitude: Double,
  val longitude: Double,
  val precision: Int,
  val sequenceLabel: String,
  val speed: Int,
)
