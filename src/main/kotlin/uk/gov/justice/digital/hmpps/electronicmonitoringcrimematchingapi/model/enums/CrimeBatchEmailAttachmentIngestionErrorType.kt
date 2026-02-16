package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums

enum class CrimeBatchEmailAttachmentIngestionErrorType(val message: String) {
  INVALID_COLUMN_COUNT("Incorrect number of columns"),
  INVALID_TEXT("Must be text"),
  INVALID_NUMBER("Must be number"),
  INVALID_DATE_FORMAT("must be a date with format yyyyMMddHHmmss"),
  CRIME_DATE_TIME_TO_AFTER_FROM("Crime date time to must be after crime date time from"),
  CRIME_DATE_TIME_EXCEEDS_WINDOW("Crime date time window must not exceed CRIME_DATE_WINDOW_HOURS hours"),
  INVALID_ENUM("ENUM must be a valid value"),
  MISSING_LOCATION_DATA("No location data present"),
  DEPENDENT_LOCATION_DATA("Dependent location data field must be provided"),
  MULTIPLE_LOCATION_DATA_TYPES("Only one location data type should be provided"),
  INVALID_LOCATION_DATA_RANGE("Only one location data type should be provided"),
  MANDATORY_PLACEHOLDER("Missing mandatory field"),
}