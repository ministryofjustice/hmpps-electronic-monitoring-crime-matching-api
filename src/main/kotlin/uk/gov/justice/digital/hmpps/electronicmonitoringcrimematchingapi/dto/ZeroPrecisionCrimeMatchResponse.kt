package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

data class ZeroPrecisionCrimeMatchResponse(
  val policeForceArea: String,
  val batchId: String,
  val crimeReference: String,
  val deviceSerialNumber: String,
  val matchedAt: String,
  val zeroPrecisionPositionCount: Long,
)
