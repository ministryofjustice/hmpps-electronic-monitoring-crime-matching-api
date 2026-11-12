package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.testutils

data class PositionSegmentsRequest(
  val deviceId: String,
  val startTime: String,
  val endTime: String,
)
