package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch

import jakarta.validation.Validator
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.data.CsvConfig.CrimeBatchCsvConfig
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeRecordDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.GeodeticDatum
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.validation.FieldValidationResult
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.validation.ValidationResult
import java.io.InputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class CrimeBatchCsvService(
  private val validator: Validator,
) {

  fun parseCsvFile(inputStream: InputStream): Pair<List<CrimeRecordDto>, List<String>> {
    val crimes = mutableListOf<CrimeRecordDto>()
    val errors = mutableListOf<String>()
    val records = CSVParser.parse(inputStream, Charsets.UTF_8, CSVFormat.DEFAULT)

    for (record in records) {
      when (val result = parseRecord(record)) {
        is ValidationResult.Success -> crimes.add(result.value)
        is ValidationResult.Failure -> errors.addAll(result.errors)
      }
    }

    return Pair(crimes, errors)
  }

  private fun parseRecord(record: CSVRecord): ValidationResult<CrimeRecordDto> {
    if (record.size() != CrimeBatchCsvConfig.COLUMN_COUNT) {
      return ValidationResult.Failure(
        listOf("Incorrect number of columns in crime record"),
      )
    }

    val policeForce = parseEnumValue<PoliceForce>("policeForce", record.policeForce())
    val crimeTypeId = parseEnumValue<CrimeType>("crimeType", record.crimeTypeId())
    val crimeReference = parseStringValue("crimeReference", record.crimeReference().trim())
    val crimeDateFrom = parseDateValue("dateFrom", record.crimeDateTimeFrom())
    val crimeDateTo = parseDateValue("dateTo", record.crimeDateTimeTo())
    val easting = parseDoubleValue("easting", record.easting())
    val northing = parseDoubleValue("northing", record.northing())
    val latitude = parseDoubleValue("latitude", record.latitude())
    val longitude = parseDoubleValue("latitude", record.longitude())
    val datum = parseEnumValue<GeodeticDatum>("datum", record.datum())
    val crimeText = parseStringValue("crimeText", record.crimeText())

    val errors = listOf(
      policeForce,
      crimeTypeId,
      crimeReference,
      crimeDateFrom,
      crimeDateTo,
      easting,
      northing,
      latitude,
      longitude,
      datum,
      crimeText,
    )
      .mapNotNull { it.errorMessage }

    if (errors.isNotEmpty()) {
      return ValidationResult.Failure(errors)
    }

    val crimeRecordDto = CrimeRecordDto(
      policeForce.value!!,
      crimeTypeId.value!!,
      crimeReference.value!!,
      crimeDateFrom.value!!,
      crimeDateTo.value!!,
      easting.value,
      northing.value,
      latitude.value,
      longitude.value,
      datum.value!!,
      crimeText.value!!,
    )

    val violations = validator.validate(crimeRecordDto)

    if (violations.isNotEmpty()) {
      return ValidationResult.Failure(violations.map { it.message })
    }

    return ValidationResult.Success(crimeRecordDto)
  }

  private fun parseStringValue(fieldName: String, value: String): FieldValidationResult<String> = try {
    FieldValidationResult(
      value = value.trim(),
    )
  } catch (_: Exception) {
    FieldValidationResult(
      errorMessage = "$fieldName must be text but was '$value'.",
    )
  }

  private fun parseDoubleValue(fieldName: String, value: String): FieldValidationResult<Double> = try {
    FieldValidationResult(
      value = if (value.trim().isBlank()) null else value.trim().toDouble(),
    )
  } catch (_: Exception) {
    FieldValidationResult(
      errorMessage = "$fieldName must be a number but was '$value'.",
    )
  }

  private fun parseDateValue(
    fieldName: String,
    value: String,
  ): FieldValidationResult<LocalDateTime> = try {
    FieldValidationResult(
      value = LocalDateTime.parse(value.trim(), DateTimeFormatter.ofPattern("yyyyMMddHHmmss")),
    )
  } catch (_: Exception) {
    FieldValidationResult(
      errorMessage = "$fieldName must be a date with format yyyyMMddHHmmss but was '$value'.",
    )
  }

  private inline fun <reified T : Enum<T>> parseEnumValue(
    fieldName: String,
    value: String,
  ): FieldValidationResult<T> = try {
    FieldValidationResult(
      value = enumValueOf<T>(value.trim().uppercase()),
    )
  } catch (_: Exception) {
    FieldValidationResult(
      errorMessage = "$fieldName must be one of ${enumValues<T>().joinToString { it.name }} but was '$value'.",
    )
  }

  private fun CSVRecord.policeForce() = this[CrimeBatchCsvConfig.ColumnsIndices.POLICE_FORCE]
  private fun CSVRecord.crimeTypeId() = this[CrimeBatchCsvConfig.ColumnsIndices.CRIME_TYPE_ID]
  private fun CSVRecord.crimeReference() = this[CrimeBatchCsvConfig.ColumnsIndices.CRIME_REFERENCE]
  private fun CSVRecord.crimeDateTimeFrom() = this[CrimeBatchCsvConfig.ColumnsIndices.CRIME_DATE_FROM]
  private fun CSVRecord.crimeDateTimeTo() = this[CrimeBatchCsvConfig.ColumnsIndices.CRIME_DATE_TO]
  private fun CSVRecord.easting() = this[CrimeBatchCsvConfig.ColumnsIndices.EASTING]
  private fun CSVRecord.northing() = this[CrimeBatchCsvConfig.ColumnsIndices.NORTHING]
  private fun CSVRecord.latitude() = this[CrimeBatchCsvConfig.ColumnsIndices.LATITUDE]
  private fun CSVRecord.longitude() = this[CrimeBatchCsvConfig.ColumnsIndices.LONGITUDE]
  private fun CSVRecord.datum() = this[CrimeBatchCsvConfig.ColumnsIndices.DATUM]
  private fun CSVRecord.crimeText() = this[CrimeBatchCsvConfig.ColumnsIndices.CRIME_TEXT]
}
