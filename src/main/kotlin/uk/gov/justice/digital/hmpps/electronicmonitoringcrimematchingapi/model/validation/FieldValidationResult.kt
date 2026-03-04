package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.validation

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeBatchEmailAttachmentIngestionErrorType

data class FieldValidationResult<T>(
  val value: T? = null,
  val errorType: CrimeBatchEmailAttachmentIngestionErrorType? = null,
  val field: String? = null,
  val input: String? = null,
)
