package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.validation

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeBatchEmailAttachmentIngestionErrorType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeType

data class EmailAttachmentIngestionError(
  val rowNumber: Long,
  val crimeReference: String?,
  val crimeTypeId: CrimeType?,
  val errorType: CrimeBatchEmailAttachmentIngestionErrorType,
  val field: String? = null,
)