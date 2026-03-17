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
<<<<<<< Updated upstream
  val submitted: Int,
  val successful: Int,
  val failed: Int,
  val crimesByCrimeType: List<IngestionAttemptCrimesByTypeResponse>,
  val validationErrors: List<CrimeBatchEmailAttachmentErrorResponse>,
=======
  val isCrimeBatch: Boolean,
  val failureSubCategory: String?,
  val submittedCount: Long?,
  val ingestedCount: Long?,
  val failedCount: Long?,
  val breakdownByCrimeType: List<BreakdownByCrimeTypeResponse>,
  val validationErrors: List<ValidationErrorResponse>,
)

data class BreakdownByCrimeTypeResponse(
  val crimeType: String,
  val submitted: Long,
  val ingested: Long,
  val failedValidation: Long,
)

data class ValidationErrorResponse(
  val crimeReference: String,
  val errorType: String,
  val requiredAction: String,
>>>>>>> Stashed changes
)
