package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch

import jakarta.validation.ValidationException
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
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private const val CRIME_DATE_WINDOW_HOURS = 12

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

    if (crimes.isNotEmpty() && crimes.map { it.policeForce }.distinct().size != 1) {
      throw ValidationException("Multiple police forces found in csv file")
    }

    return Pair(crimes, errors)
  }

  private fun parseRecord(record: CSVRecord): ValidationResult<CrimeRecordDto> {
    if (record.size() != CrimeBatchCsvConfig.COLUMN_COUNT) {
      return ValidationResult.Failure(
        listOf("Incorrect number of columns on row ${record.recordNumber}."),
      )
    }

    val policeForce = parseEnumValue<PoliceForce>(record.recordNumber, "policeForce", record.policeForce())
    val crimeTypeId = parseEnumValue<CrimeType>(record.recordNumber, "crimeType", record.crimeTypeId())
    val crimeReference = parseStringValue(record.recordNumber, "crimeReference", record.crimeReference().trim())
    val crimeDateFrom = parseDateValue(record.recordNumber, "dateFrom", record.crimeDateTimeFrom())
    val crimeDateTo = parseCrimeDateTo(record.recordNumber, record.crimeDateTimeTo(), crimeDateFrom)
    val easting = parseLocationValue(record.recordNumber, record.easting(), "easting", record.northing(), Pair(record.latitude(), record.longitude()), 0.0..600000.0)
    val northing = parseLocationValue(record.recordNumber, record.northing(), "northing", record.easting(), Pair(record.latitude(), record.longitude()), 0.0..1300000.0)
    val latitude = parseLocationValue(record.recordNumber, record.latitude(), "latitude", record.longitude(), Pair(record.easting(), record.northing()), 49.5..61.5)
    val longitude = parseLocationValue(record.recordNumber, record.longitude(), "longitude", record.latitude(), Pair(record.easting(), record.northing()), -8.5..2.6)
    val datum = parseEnumValue<GeodeticDatum>(record.recordNumber, "datum", record.datum())
    val crimeText = parseStringValue(record.recordNumber, "crimeText", record.crimeText())
    val locationValidation = validateLocationData(record.recordNumber, listOf(record.easting(), record.northing(), record.latitude(), record.longitude()))

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
      locationValidation,
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

  private fun parseStringValue(recordNumber: Long, fieldName: String, value: String): FieldValidationResult<String> = try {
    FieldValidationResult(
      value = value.trim(),
    )
  } catch (_: Exception) {
    FieldValidationResult(
      errorMessage = "$fieldName must be text but was '$value' on row $recordNumber.",
    )
  }

  private fun parseDoubleValue(recordNumber: Long, fieldName: String, value: String): FieldValidationResult<Double> = try {
    FieldValidationResult(
      value = if (value.trim().isBlank()) null else value.trim().toDouble(),
    )
  } catch (_: Exception) {
    FieldValidationResult(
      errorMessage = "$fieldName must be a number but was '$value' on row $recordNumber.",
    )
  }

  private fun parseDateValue(
    recordNumber: Long,
    fieldName: String,
    value: String,
  ): FieldValidationResult<LocalDateTime> = try {
    FieldValidationResult(
      value = LocalDateTime.parse(value.trim(), DateTimeFormatter.ofPattern("yyyyMMddHHmmss")),
    )
  } catch (_: Exception) {
    FieldValidationResult(
      errorMessage = "$fieldName must be a date with format yyyyMMddHHmmss but was '$value' on row $recordNumber.",
    )
  }

  private fun parseCrimeDateTo(
    recordNumber: Long,
    crimeDateTo: String,
    crimeDateFrom: FieldValidationResult<LocalDateTime>,
  ): FieldValidationResult<LocalDateTime> {
    val parsedDate = parseDateValue(recordNumber, "dateTo", crimeDateTo)

    val dateTo = parsedDate.value ?: return parsedDate
    val dateFrom = crimeDateFrom.value ?: return parsedDate

    if (dateTo.isBefore(dateFrom)) {
      return FieldValidationResult(
        errorMessage = "Crime date time to must be after crime date time from on row $recordNumber.",
      )
    }

    if (Duration.between(dateFrom, dateTo).toHours() > CRIME_DATE_WINDOW_HOURS) {
      return FieldValidationResult(
        errorMessage = "Crime date time window must not exceed $CRIME_DATE_WINDOW_HOURS hours on row $recordNumber.",
      )
    }

    return parsedDate
  }

  private inline fun <reified T : Enum<T>> parseEnumValue(
    recordNumber: Long,
    fieldName: String,
    value: String,
  ): FieldValidationResult<T> = try {
    FieldValidationResult(
      value = enumValueOf<T>(value.trim().uppercase()),
    )
  } catch (_: Exception) {
    FieldValidationResult(
      errorMessage = "$fieldName must be one of ${enumValues<T>().joinToString { it.name }} but was '$value' on row $recordNumber.",
    )
  }

  private fun parseLocationValue(
    recordNumber: Long,
    recordValue: String,
    fieldName: String,
    dependentField: String,
    opposingFields: Pair<String, String>,
    range: ClosedFloatingPointRange<Double>,
  ): FieldValidationResult<Double> {
    val parsedDouble = parseDoubleValue(recordNumber, fieldName, recordValue)
    val parsedDoubleValue = parsedDouble.value ?: return parsedDouble

    if (dependentField.isBlank()) {
      return FieldValidationResult(
        errorMessage = "Dependent location data field must be provided when using $fieldName on row $recordNumber.",
      )
    }

    if (!opposingFields.first.isBlank() || !opposingFields.second.isBlank()) {
      return FieldValidationResult(
        errorMessage = "Only one location data type should be provided on row $recordNumber.",
      )
    }

    if (parsedDoubleValue !in range) {
      return FieldValidationResult(
        errorMessage = "$fieldName value '$parsedDoubleValue' outside of valid range on row $recordNumber.",
      )
    }

    return parsedDouble
  }

  private fun validateLocationData(recordNumber: Long, locationValues: List<String>): FieldValidationResult<Double> {
    if (locationValues.all { it.trim().isBlank() }) {
      return FieldValidationResult(errorMessage = "No location data present on row $recordNumber.")
    }

    return FieldValidationResult()
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
