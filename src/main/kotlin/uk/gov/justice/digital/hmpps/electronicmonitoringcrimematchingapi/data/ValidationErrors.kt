package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.data

class ValidationErrors {
  object Crime {
    const val INVALID_CRIME_REFERENCE: String = "A crime reference must be provided"
    const val INVALID_CRIME_DATE: String = "A valid crime date range must be provided"
    const val EASTING_MIN: String = "Easting value cannot be below the minimum of 0"
    const val EASTING_MAX: String = "Easting value cannot be above the maximum of 600000"
    const val NORTHING_MIN: String = "Northing value cannot be below the minimum of 0"
    const val NORTHING_MAX: String = "Northing value cannot be above the maximum of 1300000"
    const val LATITUDE_MIN: String = "Latitude value cannot be below the minimum of 49.5"
    const val LATITUDE_MAX: String = "Latitude value cannot be above the maximum of 61.5"
    const val LONGITUDE_MIN: String = "Longitude value cannot be below the minimum of -8.5"
    const val LONGITUDE_MAX: String = "Longitude value cannot be above the maximum of 2.6"
    const val INVALID_LOCATION_DATA: String = "One location data type required"
  }
}
