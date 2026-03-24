package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums

enum class CrimeBatchEmailAttachmentIngestionErrorType(val message: String, val requiredAction: String) {
  INVALID_BATCH_ID_FORMAT("Invalid Batch ID format", "Amend formatting issues"),
  INVALID_BATCH_ID_DATE("Invalid date in Batch ID", "Amend formatting issues"),
  INVALID_COLUMN_COUNT("Incorrect number of columns", "Amend formatting issues"),
  INVALID_TEXT("Field must be text", "Amend formatting issues"),
  INVALID_NUMBER("Field must be number", "Amend formatting issues"),
  INVALID_POLICE_FORCE("Field must be a valid ENUM value", "Amend police force to a registered force"),
  INVALID_CRIME_TYPE("Field must be a valid ENUM value", "Amend crime type to a registered crime type"),
  INVALID_FROM_DATE_FORMAT("Field must be a date with format yyyyMMddHHmmss", "Amend from date/time to format yyyyMMddHHmmss"),
  INVALID_TO_DATE_FORMAT("Field must be a date with format yyyyMMddHHmmss", "Amend to date/time to format yyyyMMddHHmmss"),
  CRIME_DATE_TIME_TO_AFTER_FROM("Crime date time to must be after crime date time from", "Ensure from date/time precedes to date/time"),
  CRIME_DATE_TIME_EXCEEDS_WINDOW("Crime date time window must not exceed valid window", "Amend formatting issues"),
  MISSING_LOCATION_DATA("No location data present", "Provide location data"),
  MISSING_BATCH_ID("Batch ID must be present", "Amend formatting issues"),
  MISSING_CRIME_REFERENCE("Crime reference must be present", "Provide the missing test reference"),
  DEPENDENT_LOCATION_DATA("Dependent location data field must be provided", "Provide the missing field value"),
  MULTIPLE_LOCATION_DATA_TYPES("Only one location data type should be provided", "Amend formatting issues"),
  INVALID_LOCATION_DATA_RANGE("Location data range exceeds valid range", "Co-ordinates outside of valid range"),
}
