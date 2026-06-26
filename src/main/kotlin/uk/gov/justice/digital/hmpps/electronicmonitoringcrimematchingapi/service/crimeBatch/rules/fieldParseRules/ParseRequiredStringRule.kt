package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch.rules.fieldParseRules

import org.apache.commons.csv.CSVRecord
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch.rules.CrimeBatchCsvColumn
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch.rules.ValidationError
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch.rules.ValidationErrorType

class ParseRequiredStringRule(column: CrimeBatchCsvColumn) : FieldParseRule<String>(column) {
  override val id = "REQUIRED_STRING_${column.name}"

  override fun parse(row: CSVRecord): FieldParseResult<String> {
    val rawValue = row[column.index]
    val trimmedValue = rawValue.trim()

    if (trimmedValue.isBlank()) {
      return FieldParseResult.Failure(
        ValidationError(
          ruleId = id,
          errorType = ValidationErrorType.MISSING,
          column = column,
          value = rawValue,
        ),
      )
    }

    return FieldParseResult.Success(trimmedValue)
  }
}


