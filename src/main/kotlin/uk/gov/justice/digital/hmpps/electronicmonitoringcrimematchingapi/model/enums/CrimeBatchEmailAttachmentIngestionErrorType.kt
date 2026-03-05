package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums

enum class CrimeBatchEmailAttachmentIngestionErrorType(val message: String) {
  INVALID_BATCH_ID_FORMAT("Invalid Batch ID format"),
  INVALID_BATCH_ID_DATE("Invalid date in Batch ID"),
  INVALID_COLUMN_COUNT("Incorrect number of columns"),
  INVALID_TEXT("Field must be text"),
  INVALID_NUMBER("Field must be number"),
  INVALID_ENUM("Field must be a valid ENUM value"),
  INVALID_DATE_FORMAT("Field must be a date with format yyyyMMddHHmmss"),
  CRIME_DATE_TIME_TO_AFTER_FROM("Crime date time to must be after crime date time from"),
  CRIME_DATE_TIME_EXCEEDS_WINDOW("Crime date time window must not exceed valid window"),
  MISSING_LOCATION_DATA("No location data present"),
  MISSING_BATCH_ID("Batch ID must be present"),
  MISSING_CRIME_REFERENCE("Crime reference must be present"),
  DEPENDENT_LOCATION_DATA("Dependent location data field must be provided"),
  MULTIPLE_LOCATION_DATA_TYPES("Only one location data type should be provided"),
  INVALID_LOCATION_DATA_RANGE("Location data range exceeds valid range"),
}
