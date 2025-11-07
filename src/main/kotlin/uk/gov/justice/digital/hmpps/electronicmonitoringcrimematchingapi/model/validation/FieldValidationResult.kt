package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.validation

data class FieldValidationResult<T>(
  val value: T? = null,
  val errorMessage: String? = null,
)
