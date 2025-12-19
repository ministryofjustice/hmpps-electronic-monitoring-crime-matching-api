package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.validation

data class EmailAttachmentIngestionError(
  val rowNumber: Long?,
  val crimeReference: String?,
  val errorType: String,
)
