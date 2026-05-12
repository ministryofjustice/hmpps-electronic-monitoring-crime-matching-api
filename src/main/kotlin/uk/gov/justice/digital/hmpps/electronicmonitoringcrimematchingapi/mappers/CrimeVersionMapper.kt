package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.mappers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeVersionResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.DeviceWearerPositionResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.DeviceWearerResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.MatchingResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.geo.CoordinateResolver
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.roundTo
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeMatchingResult
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeMatchingResultDeviceWearer
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeMatchingResultPosition
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeVersion

@Component
class CrimeVersionMapper(
  val coordinateResolver: CoordinateResolver,
) {

  fun toDto(crimeVersion: CrimeVersion): CrimeVersionResponse {
    val coords = coordinateResolver.toWgs84(crimeVersion.latitude, crimeVersion.longitude, crimeVersion.easting, crimeVersion.northing)
    val latestCrimeVersionId = if (!crimeVersion.isLatest) crimeVersion.crime.latestVersion.id.toString() else null

    return CrimeVersionResponse(
      crimeVersionId = crimeVersion.id.toString(),
      latestCrimeVersionId = latestCrimeVersionId,
      crimeReference = crimeVersion.crime.crimeReference,
      batchId = crimeVersion.crimeBatch.batchId,
      crimeTypeDescription = crimeVersion.crimeTypeId.value,
      crimeTypeId = crimeVersion.crimeTypeId.name,
      crimeDateTimeFrom = crimeVersion.crimeDateTimeFrom.toString(),
      crimeDateTimeTo = crimeVersion.crimeDateTimeTo.toString(),
      crimeText = crimeVersion.crimeText,
      latitude = coords.latitude.roundTo(6),
      longitude = coords.longitude.roundTo(6),
      matching = crimeVersion.matchingResults.maxByOrNull { it.createdAt }?.let { matchingResultToDto(it) },
      versionLabel = crimeVersion.versionLabel,
    )
  }

  private fun matchingResultToDto(matchingResult: CrimeMatchingResult): MatchingResponse {
    val deviceWearers = matchingResult.deviceWearers.map(this::deviceWearerToDto)

    return MatchingResponse(deviceWearers = deviceWearers)
  }

  private fun deviceWearerToDto(deviceWearer: CrimeMatchingResultDeviceWearer): DeviceWearerResponse {
    val positions = deviceWearer.positions.map(this::deviceWearerPositionToDto).sortedBy { it.capturedDateTime }

    val deviceWearer = DeviceWearerResponse(
      address = deviceWearer.address,
      dateOfBirth = deviceWearer.dateOfBirth.toString(),
      deviceId = deviceWearer.deviceId,
      name = deviceWearer.name,
      nomisId = deviceWearer.nomisId,
      pncRef = deviceWearer.pncRef,
    )
    deviceWearer.positions.addAll(positions)
    return deviceWearer
  }

  private fun deviceWearerPositionToDto(position: CrimeMatchingResultPosition): DeviceWearerPositionResponse = DeviceWearerPositionResponse(
    latitude = position.latitude,
    longitude = position.longitude,
    sequenceLabel = position.sequenceLabel,
    precision = position.precision,
    capturedDateTime = position.capturedDateTime.toString(),
    direction = position.direction,
    speed = position.speed,
  )
}
