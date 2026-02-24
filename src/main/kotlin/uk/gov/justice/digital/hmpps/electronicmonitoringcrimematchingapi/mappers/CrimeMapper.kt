package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.mappers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.geo.CoordinateResolver
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.roundTo
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeVersion

@Component
class CrimeMapper(
  val coordinateResolver: CoordinateResolver,
) {
  fun toDto(version: CrimeVersion): CrimeResponse {
    val coords = coordinateResolver.toWgs84(version.latitude, version.longitude, version.easting, version.northing)

    return CrimeResponse(
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
}
