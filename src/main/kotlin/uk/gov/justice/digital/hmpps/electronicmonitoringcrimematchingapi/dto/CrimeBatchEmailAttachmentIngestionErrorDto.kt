package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

data class CrimeBatchEmailAttachmentIngestionErrorDto(
  val rowNumber: Long?,
  val crimeReference: String?,
  val errorType: String,
)
