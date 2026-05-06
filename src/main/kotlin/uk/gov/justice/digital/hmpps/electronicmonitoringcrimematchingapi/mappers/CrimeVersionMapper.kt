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
    val versions = crimeVersion.crime.crimeVersions

    val latestVersion = versions.maxByOrNull { it.createdAt }
    val isLatest = latestVersion?.id == crimeVersion.id
    val latestCrimeVersionId = if (!isLatest) latestVersion?.id?.toString() else null

    val versionLabel = computeVersionLabel(
      isLatest,
      versions,
      crimeVersion,
    )

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
      versionLabel = versionLabel,
    )
  }

  fun computeVersionLabel(isLatest: Boolean, versions: List<CrimeVersion>, crimeVersion: CrimeVersion): String {
    var versionLabel = ""

    if (isLatest) {
      versionLabel = "Latest version"
      if (crimeVersion.updates.isEmpty() && versions.indexOf(crimeVersion) != 0) {
        versionLabel += " (Duplicate)"
      }
    } else {
      var versionNumber = 1

      for (v in versions) {
        // Only increment when there are updates
        if (v.updates.isNotEmpty()) {
          versionNumber++
        }

        // stop when we reach the current version
        if (v == crimeVersion) {
          break
        }
      }
      versionLabel = "Version $versionNumber"

      if (versionNumber > 1) {
        versionLabel += " (Duplicate)"
      }
    }

    return versionLabel
  }

  fun matchingResultToDto(matchingResult: CrimeMatchingResult): MatchingResponse {
    val deviceWearers = matchingResult.deviceWearers.map(this::deviceWearerToDto)

    return MatchingResponse(deviceWearers = deviceWearers)
  }

  fun deviceWearerToDto(deviceWearer: CrimeMatchingResultDeviceWearer): DeviceWearerResponse {
    val positions = deviceWearer.positions.map(this::deviceWearerPositionToDto).sortedBy { it.capturedDateTime }

    val deviceWearer = DeviceWearerResponse(
      deviceId = deviceWearer.deviceId,
      name = deviceWearer.name,
      nomisId = deviceWearer.nomisId,
    )
    deviceWearer.positions.addAll(positions)
    return deviceWearer
  }

  fun deviceWearerPositionToDto(position: CrimeMatchingResultPosition): DeviceWearerPositionResponse = DeviceWearerPositionResponse(
    latitude = position.latitude,
    longitude = position.longitude,
    sequenceLabel = position.sequenceLabel,
    precision = position.precision.toInt(),
    capturedDateTime = position.capturedDateTime.toString(),
    direction = position.direction.toInt(),
    speed = position.speed.toInt(),
  )
}
