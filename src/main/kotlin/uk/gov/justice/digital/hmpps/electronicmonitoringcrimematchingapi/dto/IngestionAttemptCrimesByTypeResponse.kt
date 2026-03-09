package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

data class IngestionAttemptCrimesByTypeResponse(
  val crimeType: String,
  val submitted: Int,
  val failed: Int,
  val successful: Int,
)
