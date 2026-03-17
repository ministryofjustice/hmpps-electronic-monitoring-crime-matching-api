package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

data class CrimeBatchIngestionAttemptResponse(
  val ingestionAttemptId: String,
  val ingestionStatus: String,
  val policeForceArea: String?,
  val crimeBatchId: String?,
  val batchId: String?,
  val matches: Long?,
  val createdAt: String,
  val fileName: String?,
  val submitted: Int,
  val successful: Int,
  val failed: Int,
  val crimesByCrimeType: List<IngestionAttemptCrimesByTypeResponse>,
  val validationErrors: List<CrimeBatchEmailAttachmentErrorResponse>,
)
