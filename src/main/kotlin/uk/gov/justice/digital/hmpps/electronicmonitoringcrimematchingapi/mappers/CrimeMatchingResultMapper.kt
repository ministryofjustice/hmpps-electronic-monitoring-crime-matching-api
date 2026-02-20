package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.mappers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeMatchingResultResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.geo.Osgb36ToWgs84Converter
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.Wgs84
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection.CrimeMatchingResultProjection

@Component
class CrimeMatchingResultMapper(
  val converter: Osgb36ToWgs84Converter,
) {

  fun toDto(matchingResult: CrimeMatchingResultProjection): CrimeMatchingResultResponse {
    val coords = getLatLng(matchingResult)

    return CrimeMatchingResultResponse(
      policeForce = matchingResult.policeForceArea,
      batchId = matchingResult.batchId,
      crimeRef = matchingResult.crimeReference,
      crimeType = matchingResult.crimeTypeId,
      crimeDateTimeFrom = matchingResult.crimeDateTimeFrom.toString(),
      crimeDateTimeTo = matchingResult.crimeDateTimeTo.toString(),
      crimeLatitude = coords.latitude,
      crimeLongitude = coords.longitude,
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

  private fun getLatLng(matchingResult: CrimeMatchingResultProjection): Wgs84 {
    val hasWgs84 = matchingResult.crimeLatitude != null && matchingResult.crimeLongitude != null
    val hasOsgb36 = matchingResult.crimeEasting != null && matchingResult.crimeNorthing != null

    return when {
      hasWgs84 && !hasOsgb36 -> Wgs84(longitude = matchingResult.crimeLongitude!!, latitude = matchingResult.crimeLatitude!!)
      hasOsgb36 && !hasWgs84 -> converter.convert(matchingResult.crimeEasting!!, matchingResult.crimeNorthing!!)
      else -> throw IllegalStateException(
        "Crime must have either (lat,lon) or (easting,northing). Got lat=${matchingResult.crimeLatitude} lon=${matchingResult.crimeLongitude} e=${matchingResult.crimeEasting} n=${matchingResult.crimeNorthing}",
      )
    }
  }
}
