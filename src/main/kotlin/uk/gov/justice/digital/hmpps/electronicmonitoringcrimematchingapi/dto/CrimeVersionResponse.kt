package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

data class CrimeVersionResponse(
  val crimeVersionId: String,
  val crimeReference: String,
  val crimeType: String,
  val crimeTypeId: String,
  val crimeDateTimeFrom: String,
  val crimeDateTimeTo: String,
  val crimeText: String,
  val longitude: Double,
  val latitude: Double,
  val matching: MatchingResponse?,
  val versionLabel: String,
)
