package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums

enum class CrimeVersionFieldName(val value: String) {
  CRIME_TYPE_ID("Crime type"),
  CRIME_DATE_TIME_FROM("Crime date time from"),
  CRIME_DATE_TIME_TO("Crime date time to"),
  EASTING("Easting"),
  NORTHING("Northing"),
  LONGITUDE("Longitude"),
  LATITUDE("Latitude"),
  CRIME_TEXT("Crime text"),
  ;

  fun groupLabel(): String? = when (this) {
    EASTING, NORTHING, LATITUDE, LONGITUDE -> "Crime location"
    CRIME_DATE_TIME_FROM, CRIME_DATE_TIME_TO -> "Crime date"
    else -> null
  }
}
