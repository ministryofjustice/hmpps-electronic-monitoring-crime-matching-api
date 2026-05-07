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
    val versions = crimeVersion.crime.crimeVersions.sortedBy { it.createdAt }

    val latestVersion = versions.lastOrNull()
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

  // Builds the label based on version history, only incrementing the version number if there are updates
  fun computeVersionLabel(
    isLatest: Boolean,
    versions: List<CrimeVersion>,
    current: CrimeVersion,
  ): String {
    val currentVersionIndex = versions.indexOf(current)

    val versionNumber = versions
      .take(currentVersionIndex + 1)
      .count { it.updates.isNotEmpty() } + 1

    val previousVersionNumber = if (currentVersionIndex > 0) {
      versions
        .take(currentVersionIndex)
        .count { it.updates.isNotEmpty() } + 1
    } else {
      null
    }

    val isDuplicate = previousVersionNumber != null && versionNumber == previousVersionNumber

    return if (isLatest) {
      buildString {
        append("Latest version")
        if (isDuplicate) append(" (Duplicate)")
      }
    } else {
      buildString {
        append("Version $versionNumber")
        if (isDuplicate) append(" (Duplicate)")
      }
    }
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
    precision = position.precision,
    capturedDateTime = position.capturedDateTime.toString(),
    direction = position.direction,
    speed = position.speed,
  )
}
