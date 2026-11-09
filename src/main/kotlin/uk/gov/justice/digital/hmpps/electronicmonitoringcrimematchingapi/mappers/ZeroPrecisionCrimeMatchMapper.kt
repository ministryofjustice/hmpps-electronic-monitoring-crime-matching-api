package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.mappers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.ZeroPrecisionCrimeMatchResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection.ZeroPrecisionCrimeMatchProjection
import java.time.ZoneOffset

@Component
class ZeroPrecisionCrimeMatchMapper {
  fun toDto(projection: ZeroPrecisionCrimeMatchProjection): ZeroPrecisionCrimeMatchResponse = ZeroPrecisionCrimeMatchResponse(
    policeForceArea = projection.policeForceArea,
    batchId = projection.batchId,
    crimeReference = projection.crimeReference,
    deviceSerialNumber = projection.deviceSerialNumber,
    matchedAt = projection.matchedAt.toInstant(ZoneOffset.UTC).toString(),
    zeroPrecisionPositionCount = projection.zeroPrecisionPositionCount,
  )
}
