package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

data class DeviceWearerPositionResponse(
  val latitude: Double,
  val longitude: Double,
  val sequenceLabel: String,
  val confidence: Int,
  val capturedDateTime: String,
)
