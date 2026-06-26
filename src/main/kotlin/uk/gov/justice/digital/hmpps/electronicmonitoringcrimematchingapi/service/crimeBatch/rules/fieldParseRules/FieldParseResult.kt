package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch.rules.fieldParseRules

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch.rules.ValidationError

sealed interface FieldParseResult<out T> {
  data class Success<T>(val value: T) : FieldParseResult<T>
  data class Failure(val error: ValidationError) : FieldParseResult<Nothing>
}
