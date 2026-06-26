package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch.rules

enum class ValidationErrorType {
  MISSING,
  NOT_IN_APPROVED_LIST,
}

data class ValidationError(
  val ruleId: String,
  val errorType: ValidationErrorType,
  val column: CrimeBatchCsvColumn,
  val value: String? = null,
)
