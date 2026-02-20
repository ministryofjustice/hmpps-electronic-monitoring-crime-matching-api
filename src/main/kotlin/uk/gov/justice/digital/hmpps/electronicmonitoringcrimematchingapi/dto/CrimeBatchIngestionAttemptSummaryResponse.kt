package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

data class CrimeBatchIngestionAttemptSummaryResponse(
  val ingestionAttemptId: String,
  val status: String,
  val policeForce: String?,
  val batchId: String?,
  val matches: Long?,
  val createdAt: String?,
)
