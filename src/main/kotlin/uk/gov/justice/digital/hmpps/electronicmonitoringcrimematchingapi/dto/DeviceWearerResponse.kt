package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

data class DeviceWearerResponse(
  val name: String,
  val deviceId: Long,
  val nomisId: String,
  val positions: MutableList<DeviceWearerPositionResponse> = mutableListOf(),
)
