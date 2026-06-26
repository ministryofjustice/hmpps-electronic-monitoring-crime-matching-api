package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch.rules.fieldParseRules

import org.apache.commons.csv.CSVRecord
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.data.CsvConfig.CrimeBatchCsvConfig
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch.rules.CrimeBatchCsvColumn

abstract class FieldParseRule<T>(
  val column: CrimeBatchCsvColumn,
) {
  abstract val id: String

  abstract fun parse(row: CSVRecord): FieldParseResult<T>

  private fun CSVRecord.policeForce() = this[CrimeBatchCsvConfig.ColumnsIndices.POLICE_FORCE]
  private fun CSVRecord.crimeTypeId() = this[CrimeBatchCsvConfig.ColumnsIndices.CRIME_TYPE_ID]
  private fun CSVRecord.batchId() = this[CrimeBatchCsvConfig.ColumnsIndices.BATCH_ID]
  private fun CSVRecord.crimeReference() = this[CrimeBatchCsvConfig.ColumnsIndices.CRIME_REFERENCE]
  private fun CSVRecord.crimeDateTimeFrom() = this[CrimeBatchCsvConfig.ColumnsIndices.CRIME_DATE_FROM]
  private fun CSVRecord.crimeDateTimeTo() = this[CrimeBatchCsvConfig.ColumnsIndices.CRIME_DATE_TO]
  private fun CSVRecord.easting() = this[CrimeBatchCsvConfig.ColumnsIndices.EASTING]
  private fun CSVRecord.northing() = this[CrimeBatchCsvConfig.ColumnsIndices.NORTHING]
  private fun CSVRecord.latitude() = this[CrimeBatchCsvConfig.ColumnsIndices.LATITUDE]
  private fun CSVRecord.longitude() = this[CrimeBatchCsvConfig.ColumnsIndices.LONGITUDE]
  private fun CSVRecord.crimeText() = this[CrimeBatchCsvConfig.ColumnsIndices.CRIME_TEXT]
}
