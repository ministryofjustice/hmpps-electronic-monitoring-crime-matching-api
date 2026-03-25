package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

data class CrimeVersionResponse(
  val crimeVersionId: String,
  val crimeReference: String,
  val crimeType: String,
  val crimeDateTimeFrom: String,
  val crimeDateTimeTo: String,
  val crimeText: String,
  val matching: MatchingResponse?,
  val versionLabel: String,
)
