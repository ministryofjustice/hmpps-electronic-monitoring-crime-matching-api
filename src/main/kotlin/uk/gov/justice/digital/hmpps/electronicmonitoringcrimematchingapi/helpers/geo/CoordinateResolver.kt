package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.geo

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.Wgs84

class CoordinateResolver(
  val converter: Osgb36ToWgs84Converter,
) {
  fun toWgs84(latitude: Double?, longitude: Double?, easting: Double?, northing: Double?): Wgs84 {
    val hasWgs84 = latitude != null && longitude != null
    val hasOsgb36 = easting != null && northing != null

    return when {
      hasWgs84 && !hasOsgb36 -> Wgs84(longitude = longitude, latitude = latitude)
      hasOsgb36 && !hasWgs84 -> converter.convert(easting, northing)
      else -> throw IllegalStateException(
        "Crime must have either (lat,lon) or (easting,northing). Got lat=$latitude lon=$longitude e=$easting n=$northing",
      )
    }
  }
}
