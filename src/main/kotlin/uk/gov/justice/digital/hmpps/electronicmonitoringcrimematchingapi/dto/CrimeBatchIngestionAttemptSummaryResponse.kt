package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

data class CrimeBatchIngestionAttemptSummaryResponse(
  val ingestionAttemptId: String,
  val ingestionStatus: String,
  val policeForceArea: String,
  val batchId: String,
  val matches: Long?,
  val createdAt: String,
)
