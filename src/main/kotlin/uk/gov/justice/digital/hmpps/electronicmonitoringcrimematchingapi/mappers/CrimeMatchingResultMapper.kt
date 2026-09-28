package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.mappers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeMatchingResultResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.geo.CoordinateResolver
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.roundTo
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection.CrimeMatchingResultProjection

@Component
class CrimeMatchingResultMapper(
  val coordinateResolver: CoordinateResolver,
) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  fun toDto(matchingResult: CrimeMatchingResultProjection): CrimeMatchingResultResponse {
    val coords = coordinateResolver.toWgs84(matchingResult.crimeLatitude, matchingResult.crimeLongitude, matchingResult.crimeEasting, matchingResult.crimeNorthing)

    val crimeTypeDescription = try {
      CrimeType.from(matchingResult.crimeTypeId).value
    } catch (e: NoSuchElementException) {
      logger.warn("Crime type ID '${matchingResult.crimeTypeId}' is not a valid CrimeType enum value", e)
      ""
    }

    return CrimeMatchingResultResponse(
      policeForce = matchingResult.policeForceArea,
      batchId = matchingResult.batchId,
      crimeRef = matchingResult.crimeReference,
      crimeType = crimeTypeDescription,
      crimeDateTimeFrom = matchingResult.crimeDateTimeFrom.toString(),
      crimeDateTimeTo = matchingResult.crimeDateTimeTo.toString(),
      crimeLatitude = coords.latitude.roundTo(8),
      crimeLongitude = coords.longitude.roundTo(8),
      crimeText = matchingResult.crimeText,
      deviceId = matchingResult.deviceId,
      deviceSerialNumber = matchingResult.deviceSerialNumber,
      deviceName = matchingResult.deviceName,
      subjectId = matchingResult.identifier,
      subjectName = matchingResult.name,
      subjectNomisId = matchingResult.nomisId,
      subjectPncRef = matchingResult.pncRef,
      subjectAddress = matchingResult.address,
      subjectDateOfBirth = matchingResult.dateOfBirth?.toString() ?: "",
      subjectManager = "", // Always an empty string
    )
  }
}
