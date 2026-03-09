package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

data class CrimeBatchEmailAttachmentErrorResponse(
  val errorType: String,
  val fieldName: String,
  val value: String,
  val crimeReference: String,
  val rowNumber: String,
)
