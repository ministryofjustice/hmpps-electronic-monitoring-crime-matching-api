package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.mappers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.geo.Osgb36ToWgs84Converter
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.roundTo
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.Wgs84
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeVersion
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.Datum

@Component
class CrimeMapper(
  val converter: Osgb36ToWgs84Converter,
) {
  fun toDto(version: CrimeVersion): CrimeDto {
    val coords = getLatLng(version)

    return CrimeDto(
      id = version.id.toString(),
      crimeTypeId = version.crimeTypeId.name,
      crimeTypeDescription = version.crimeTypeId.value,
      policeForce = version.crime.policeForceArea.name,
      crimeReference = version.crime.crimeReference,
      crimeDateTimeFrom = version.crimeDateTimeFrom.toString(),
      crimeDateTimeTo = version.crimeDateTimeTo.toString(),
      latitude = coords.latitude.roundTo(6),
      longitude = coords.longitude.roundTo(6),
      crimeText = version.crimeText,
    )
  }

  private fun getLatLng(version: CrimeVersion): Wgs84 {
    if (version.datum == Datum.WGS84) {
      return Wgs84(longitude = version.longitude!!, latitude = version.latitude!!)
    }

    return converter.convert(version.easting!!, version.northing!!)
  }
}
