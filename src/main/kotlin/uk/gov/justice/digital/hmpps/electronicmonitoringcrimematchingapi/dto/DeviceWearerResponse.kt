package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

data class DeviceWearerResponse(
  val address: String,
  val dateOfBirth: String,
  val name: String,
  val deviceId: Long,
  val nomisId: String,
  val pncRef: String,
  val positions: MutableList<DeviceWearerPositionResponse> = mutableListOf(),
)
