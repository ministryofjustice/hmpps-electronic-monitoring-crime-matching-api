package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

data class FailedCrimeSubmissionResponse(
  val total: Long,
  val failures: List<AttachmentIngestionErrorResponse>,
)
