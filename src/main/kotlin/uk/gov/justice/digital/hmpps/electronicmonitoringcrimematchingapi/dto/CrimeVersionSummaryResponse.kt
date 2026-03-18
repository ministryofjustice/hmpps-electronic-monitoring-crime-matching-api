package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

data class CrimeVersionSummaryResponse(
  val crimeVersionId: String,
  val crimeReference: String,
  val policeForceArea: String,
  val crimeType: String,
  val crimeDate: String,
  val batchId: String,
  val ingestionDateTime: String,
  val matched: Boolean,
  val versionLabel: String,
  val updates: String,
)
