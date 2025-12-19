package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.validation

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeBatchEmailAttachmentIngestionErrorDto

sealed class ValidationResult<out T> {
  data class Success<out T>(val value: T) : ValidationResult<T>()
  data class Failure(val errors: List<CrimeBatchEmailAttachmentIngestionErrorDto>) : ValidationResult<Nothing>()
}
