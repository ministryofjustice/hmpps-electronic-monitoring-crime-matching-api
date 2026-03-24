package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

data class CrimeBatchEmailAttachmentErrorResponse(
  val crimeReference: String,
  val errorType: String,
  val requiredAction: String,
)
