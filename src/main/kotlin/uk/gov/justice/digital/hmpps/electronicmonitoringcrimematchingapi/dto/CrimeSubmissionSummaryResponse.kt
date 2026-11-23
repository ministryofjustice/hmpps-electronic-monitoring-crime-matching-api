package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

class CrimeSubmissionSummaryResponse(
  val crimeType: String,
  val submitted: Long,
  val ingested: Long,
  val failed: Long,
  val notIngestedPercentage: Double,
)
