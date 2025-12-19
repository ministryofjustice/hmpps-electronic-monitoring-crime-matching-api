package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.validation

sealed class ValidationResult<out T> {
  data class Success<out T>(val value: T) : ValidationResult<T>()
  data class Failure(val errors: List<EmailAttachmentIngestionError>) : ValidationResult<Nothing>()
}
