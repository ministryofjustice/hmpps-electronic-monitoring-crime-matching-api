package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.mappers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeVersionResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.DeviceWearerPositionResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.DeviceWearerResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.MatchingResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.geo.CoordinateResolver
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.roundTo
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection.CrimeVersionProjection

@Component
class CrimeVersionMapper(
  val coordinateResolver: CoordinateResolver,
) {

  fun toDto(results: List<CrimeVersionProjection>): CrimeVersionResponse {
    val crimeVersion = results.first()
    val coords = coordinateResolver.toWgs84(crimeVersion.crimeLatitude, crimeVersion.crimeLongitude, crimeVersion.crimeEasting, crimeVersion.crimeNorthing)

    val deviceWearerMap = LinkedHashMap<String, DeviceWearerResponse>()
    results.forEach { row ->
      if (row.deviceWearerId != null) {
        val wearer = deviceWearerMap.getOrPut(row.deviceWearerId!!) {
          DeviceWearerResponse(
            name = row.name!!,
            deviceId = row.deviceId!!,
            nomisId = row.nomisId!!,
          )
        }

        wearer.positions += DeviceWearerPositionResponse(
          latitude = row.wearerLatitude!!,
          longitude = row.wearerLongitude!!,
          sequenceLabel = row.sequenceLabel!!,
          confidence = row.confidence!!,
          capturedDateTime = row.capturedDateTime.toString(),
        )
      }
    }

    val matchingResponse = MatchingResponse(deviceWearers = deviceWearerMap.values.toList())

    return CrimeVersionResponse(
      crimeVersionId = crimeVersion.crimeVersionId.toString(),
      crimeReference = crimeVersion.crimeReference,
      crimeType = crimeVersion.crimeType.value,
      crimeDateTimeFrom = crimeVersion.crimeDateTimeFrom.toString(),
      crimeDateTimeTo = crimeVersion.crimeDateTimeTo.toString(),
      crimeText = crimeVersion.crimeText,
      latitude = coords.latitude.roundTo(6),
      longitude = coords.longitude.roundTo(6),
      matching = if (crimeVersion.matchingResultId != null) matchingResponse else null,
      versionLabel = "Latest version",
    )
  }
}
