package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection

import java.time.LocalDateTime

interface ZeroPrecisionCrimeMatchProjection {
  val policeForceArea: String
  val batchId: String
  val crimeReference: String
  val deviceSerialNumber: String
  val matchedAt: LocalDateTime
  val zeroPrecisionPositionCount: Long
}
