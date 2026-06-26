package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch.rules.fieldParseRules

import org.apache.commons.csv.CSVRecord
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch.rules.CrimeBatchCsvColumn
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch.rules.ValidationError
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch.rules.ValidationErrorType

class ParseRequiredPoliceForceRule(column: CrimeBatchCsvColumn) : FieldParseRule<PoliceForce>(column) {
  override val id = "REQUIRED_POLICE_FORCE"

  override fun parse(row: CSVRecord): FieldParseResult<PoliceForce> {
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

    val policeForce = PoliceForce.fromOrNull(trimmedValue) ?: return FieldParseResult.Failure(
      ValidationError(
        ruleId = id,
        errorType = ValidationErrorType.NOT_IN_APPROVED_LIST,
        column = column,
        value = rawValue,
      ),
    )

    return FieldParseResult.Success(policeForce)
  }
}