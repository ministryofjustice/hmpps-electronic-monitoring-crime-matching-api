package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

data class CrimeVersionResponse(
  val crimeVersionId: String,
  val latestCrimeVersionId: String?,
  val crimeReference: String,
  val policeForceArea: String,
  val batchId: String,
  val crimeTypeDescription: String,
  val crimeTypeId: String,
  val crimeDateTimeFrom: String,
  val crimeDateTimeTo: String,
  val crimeText: String,
  val longitude: Double,
  val latitude: Double,
  val matching: MatchingResponse?,
  val versionLabel: String,
)
