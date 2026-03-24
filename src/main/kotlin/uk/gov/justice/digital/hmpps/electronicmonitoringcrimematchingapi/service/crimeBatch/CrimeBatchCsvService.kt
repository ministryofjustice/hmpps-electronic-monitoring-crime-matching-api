package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.data.CsvConfig.CrimeBatchCsvConfig
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeRecordRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.ParseResult
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeBatchEmailAttachmentIngestionErrorType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.validation.EmailAttachmentIngestionError
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.validation.FieldValidationResult
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.validation.ValidationResult
import java.io.InputStream
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private const val CRIME_DATE_WINDOW_HOURS = 12

@Service
class CrimeBatchCsvService {

  fun parseCsvFile(inputStream: InputStream): ParseResult {
    val crimes = mutableListOf<CrimeRecordRequest>()
    val errors = mutableListOf<EmailAttachmentIngestionError>()
    val records = CSVParser.parse(inputStream, Charsets.UTF_8, CSVFormat.DEFAULT)
    var recordCount = 0

    for (record in records) {
      recordCount++
      when (val result = parseRecord(record)) {
        is ValidationResult.Success -> crimes.add(result.value)
        is ValidationResult.Failure -> errors.addAll(result.errors)
      }
    }

    return ParseResult(recordCount, crimes, errors)
  }

  private fun parseRecord(record: CSVRecord): ValidationResult<CrimeRecordRequest> {
    if (record.size() != CrimeBatchCsvConfig.COLUMN_COUNT) {
      return ValidationResult.Failure(
        listOf(EmailAttachmentIngestionError(rowNumber = record.recordNumber, crimeReference = null, crimeTypeId = null, errorType = CrimeBatchEmailAttachmentIngestionErrorType.INVALID_COLUMN_COUNT)),
      )
    }

    val policeForce = parseEnumValue<PoliceForce>("policeForce", record.policeForce(), CrimeBatchEmailAttachmentIngestionErrorType.INVALID_POLICE_FORCE)
    val crimeTypeId = parseEnumValue<CrimeType>("crimeType", record.crimeTypeId(), CrimeBatchEmailAttachmentIngestionErrorType.INVALID_CRIME_TYPE)
    val batchId = parseBatchId(record.batchId().trim(), policeForce.value)
    val crimeReference = parseCrimeReference(record.crimeReference().trim())
    val crimeDateFrom = parseDateValue("dateFrom", record.crimeDateTimeFrom(), CrimeBatchEmailAttachmentIngestionErrorType.INVALID_FROM_DATE_FORMAT)
    val crimeDateTo = parseCrimeDateTo(record.crimeDateTimeTo(), crimeDateFrom)
    val easting = parseLocationValue(record.easting(), "easting", record.northing(), Pair(record.latitude(), record.longitude()), 0.0..600000.0)
    val northing = parseLocationValue(record.northing(), "northing", record.easting(), Pair(record.latitude(), record.longitude()), 0.0..1300000.0)
    val latitude = parseLocationValue(record.latitude(), "latitude", record.longitude(), Pair(record.easting(), record.northing()), 49.5..61.5)
    val longitude = parseLocationValue(record.longitude(), "longitude", record.latitude(), Pair(record.easting(), record.northing()), -8.5..2.6)
    val crimeText = parseStringValue("crimeText", record.crimeText())
    val locationValidation = validateLocationData(listOf(record.easting(), record.northing(), record.latitude(), record.longitude()))

    val errors = listOf(
      policeForce,
      crimeTypeId,
      batchId,
      crimeReference,
      crimeDateFrom,
      crimeDateTo,
      easting,
      northing,
      latitude,
      longitude,
      crimeText,
      locationValidation,
    ).mapNotNull { validation ->
      validation.errorType?.let {
        EmailAttachmentIngestionError(
          record.recordNumber,
          crimeReference.value,
          crimeTypeId.value,
          it,
          validation.field,
          validation.input,
        )
      }
    }

    if (errors.isNotEmpty()) {
      return ValidationResult.Failure(errors)
    }

    val crimeRecordDto = CrimeRecordRequest(
      policeForce.value!!,
      crimeTypeId.value!!,
      batchId.value!!,
      crimeReference.value!!,
      crimeDateFrom.value!!,
      crimeDateTo.value!!,
      easting.value,
      northing.value,
      latitude.value,
      longitude.value,
      crimeText.value!!,
    )

    return ValidationResult.Success(crimeRecordDto)
  }

  private fun parseBatchId(value: String, policeForce: PoliceForce?): FieldValidationResult<String> {
    val fieldName = "batchId"
    // String validity
    val parsed = parseStringValue(fieldName, value)

    if (policeForce == null) return parsed

    if (parsed.value.isNullOrBlank()) {
      return FieldValidationResult(
        errorType = CrimeBatchEmailAttachmentIngestionErrorType.MISSING_BATCH_ID,
        field = fieldName,
      )
    }

    // Batch id pattern validity
    val regex = Regex("^${policeForce.code}(\\d{8})")
    val match = regex.find(parsed.value) ?: return FieldValidationResult(
      errorType = CrimeBatchEmailAttachmentIngestionErrorType.INVALID_BATCH_ID_FORMAT,
      field = fieldName,
      input = value,
    )

    // Batch id date validity
    val batchIdDate = match.groupValues[1]

    try {
      LocalDate.parse(batchIdDate, DateTimeFormatter.BASIC_ISO_DATE)
      return parsed
    } catch (e: DateTimeParseException) {
      return FieldValidationResult(
        errorType = CrimeBatchEmailAttachmentIngestionErrorType.INVALID_BATCH_ID_DATE,
        field = fieldName,
        input = value,
      )
    }
  }

  private fun parseCrimeReference(value: String): FieldValidationResult<String> {
    val fieldName = "crimeReference"
    val parsed = parseStringValue(fieldName, value)

    if (parsed.value.isNullOrBlank()) {
      return FieldValidationResult(
        errorType = CrimeBatchEmailAttachmentIngestionErrorType.MISSING_CRIME_REFERENCE,
        field = fieldName,
      )
    }

    return parsed
  }

  private fun parseStringValue(fieldName: String, value: String): FieldValidationResult<String> = try {
    FieldValidationResult(
      value = value.trim(),
    )
  } catch (_: Exception) {
    FieldValidationResult(
      errorType = CrimeBatchEmailAttachmentIngestionErrorType.INVALID_TEXT,
      field = fieldName,
      input = value,
    )
  }

  private fun parseDoubleValue(fieldName: String, value: String): FieldValidationResult<Double> = try {
    FieldValidationResult(
      value = if (value.trim().isBlank()) null else value.trim().toDouble(),
    )
  } catch (_: Exception) {
    FieldValidationResult(
      errorType = CrimeBatchEmailAttachmentIngestionErrorType.INVALID_NUMBER,
      field = fieldName,
      input = value,
    )
  }

  private fun parseDateValue(
    fieldName: String,
    value: String,
    errorType: CrimeBatchEmailAttachmentIngestionErrorType,
  ): FieldValidationResult<Instant> = try {
    val parsedDate = LocalDateTime.parse(value.trim(), DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
    FieldValidationResult(
      value = parsedDate.toInstant(ZoneOffset.UTC),
    )
  } catch (_: Exception) {
    FieldValidationResult(
      errorType = errorType,
      field = fieldName,
      input = value,
    )
  }

  private fun parseCrimeDateTo(
    crimeDateTo: String,
    crimeDateFrom: FieldValidationResult<Instant>,
  ): FieldValidationResult<Instant> {
    val fieldName = "dateTo"
    val parsedDate = parseDateValue(fieldName, crimeDateTo, CrimeBatchEmailAttachmentIngestionErrorType.INVALID_TO_DATE_FORMAT)

    val dateTo = parsedDate.value ?: return parsedDate
    val dateFrom = crimeDateFrom.value ?: return parsedDate

    if (dateTo.isBefore(dateFrom)) {
      return FieldValidationResult(
        errorType = CrimeBatchEmailAttachmentIngestionErrorType.CRIME_DATE_TIME_TO_AFTER_FROM,
        field = fieldName,
        input = crimeDateTo,
      )
    }

    val crimeDateWindow = Duration.between(dateFrom, dateTo).toHours()

    if (crimeDateWindow > CRIME_DATE_WINDOW_HOURS) {
      return FieldValidationResult(
        errorType = CrimeBatchEmailAttachmentIngestionErrorType.CRIME_DATE_TIME_EXCEEDS_WINDOW,
        field = fieldName,
        input = crimeDateWindow.toString(),
      )
    }

    return parsedDate
  }

  private inline fun <reified T : Enum<T>> parseEnumValue(
    fieldName: String,
    value: String,
    errorType: CrimeBatchEmailAttachmentIngestionErrorType,
  ): FieldValidationResult<T> = try {
    FieldValidationResult(
      value = enumValueOf<T>(value.trim().uppercase()),
    )
  } catch (_: Exception) {
    FieldValidationResult(
      errorType = errorType,
      field = fieldName,
      input = value,
    )
  }

  private fun parseLocationValue(
    recordValue: String,
    fieldName: String,
    dependentField: String,
    opposingFields: Pair<String, String>,
    range: ClosedFloatingPointRange<Double>,
  ): FieldValidationResult<Double> {
    val parsedDouble = parseDoubleValue(fieldName, recordValue)
    val parsedDoubleValue = parsedDouble.value ?: return parsedDouble

    if (dependentField.isBlank()) {
      return FieldValidationResult(
        errorType = CrimeBatchEmailAttachmentIngestionErrorType.DEPENDENT_LOCATION_DATA,
        field = fieldName,
        input = recordValue,
      )
    }

    if (!opposingFields.first.isBlank() || !opposingFields.second.isBlank()) {
      return FieldValidationResult(
        errorType = CrimeBatchEmailAttachmentIngestionErrorType.MULTIPLE_LOCATION_DATA_TYPES,
        field = fieldName,
      )
    }

    if (parsedDoubleValue !in range) {
      return FieldValidationResult(
        errorType = CrimeBatchEmailAttachmentIngestionErrorType.INVALID_LOCATION_DATA_RANGE,
        field = fieldName,
        input = parsedDoubleValue.toString(),
      )
    }

    return parsedDouble
  }

  private fun validateLocationData(locationValues: List<String>): FieldValidationResult<Double> {
    if (locationValues.all { it.trim().isBlank() }) {
      return FieldValidationResult(errorType = CrimeBatchEmailAttachmentIngestionErrorType.MISSING_LOCATION_DATA)
    }

    return FieldValidationResult()
  }

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
