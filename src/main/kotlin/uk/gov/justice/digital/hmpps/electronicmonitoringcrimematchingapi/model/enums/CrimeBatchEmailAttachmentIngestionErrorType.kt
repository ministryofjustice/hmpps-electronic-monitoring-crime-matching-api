package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums

enum class CrimeBatchEmailAttachmentIngestionErrorType(val message: String) {
  INVALID_COLUMN_COUNT("Check the submitted CSV file and ensure all required columns are present"),
  MISSING_BATCH_ID("Provide a valid batch ID in the format <ForceCode><YYYYMMDD>"),
  INVALID_BATCH_ID_FORMAT("Ensure the valid batch ID starts with the correct force code followed by 8 digits (YYYYMMDD)"),
  INVALID_BATCH_ID_DATE("Check the date portion of the batch ID is a valid calendar date in YYYYMMDD format"),
  MISSING_CRIME_REFERENCE("Provide a crime reference for this row"),
  INVALID_TEXT("Check this field value and remove any unsupported characters"),
  INVALID_NUMBER("Ensure this field contains a valid number"),
  INVALID_DATE_FORMAT("Dated must be in yyyyMMddHHmmss format (e.g. 20260128103000)"),
  CRIME_DATE_TIME_TO_AFTER_FROM("Ensure the end date/time is after the start date/time"),
  CRIME_DATE_TIME_EXCEEDS_WINDOW("The time between start and end must be 12 hours or less"),
  INVALID_ENUM("Check the allowed values for this field and correct the entry"),
  DEPENDENT_LOCATION_DATA("Provide entries for both easting and northing or both latitude and longitude"),
  MULTIPLE_LOCATION_DATA_TYPES("Provide entries for either easting/northing or latitude/longitude ... not both"),
  INVALID_LOCATION_DATA_RANGE("Check that the coordinate values are within the expected boundries of Great Britian"),
  MISSING_LOCATION_DATA("Provide entries for either easting/northing or latitude/longitude coordinates"),
}
