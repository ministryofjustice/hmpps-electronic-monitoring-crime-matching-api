package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

data class AttachmentIngestionErrorResponse(
  val reasonForFailure: String,
  val failureCount: Int,
)
