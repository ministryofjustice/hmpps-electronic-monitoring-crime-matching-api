package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.validation

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeBatchEmailAttachmentIngestionErrorType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce

data class EmailAttachmentIngestionError(
  val rowNumber: Long,
  val crimeReference: String?,
  val crimeTypeId: CrimeType?,
  val errorType: CrimeBatchEmailAttachmentIngestionErrorType,
  val field: String? = null,
  val value: String? = null,
  val policeForceArea: PoliceForce? = null,
)
