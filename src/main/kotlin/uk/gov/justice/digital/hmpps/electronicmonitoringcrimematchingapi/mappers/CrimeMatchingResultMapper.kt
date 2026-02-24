package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.mappers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeMatchingResultResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.geo.CoordinateResolver
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.roundTo
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection.CrimeMatchingResultProjection

@Component
class CrimeMatchingResultMapper(
  val coordinateResolver: CoordinateResolver,
) {

  fun toDto(matchingResult: CrimeMatchingResultProjection): CrimeMatchingResultResponse {
    val coords = coordinateResolver.toWgs84(matchingResult.crimeLatitude, matchingResult.crimeLongitude, matchingResult.crimeEasting, matchingResult.crimeNorthing)

    return CrimeMatchingResultResponse(
      policeForce = matchingResult.policeForceArea,
      batchId = matchingResult.batchId,
      crimeRef = matchingResult.crimeReference,
      crimeType = matchingResult.crimeTypeId,
      crimeDateTimeFrom = matchingResult.crimeDateTimeFrom.toString(),
      crimeDateTimeTo = matchingResult.crimeDateTimeTo.toString(),
      crimeLatitude = coords.latitude.roundTo(6),
      crimeLongitude = coords.longitude.roundTo(6),
      crimeText = matchingResult.crimeText,
      deviceId = matchingResult.deviceId,
      deviceName = "",
      subjectId = "",
      subjectName = matchingResult.name,
      subjectNomisId = matchingResult.nomisId,
      subjectPncRef = "",
      subjectAddress = "",
      subjectDateOfBirth = "",
      subjectManager = "",
    )
  }
}
