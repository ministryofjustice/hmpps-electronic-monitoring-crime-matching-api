package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce
import java.time.LocalDateTime

interface ZeroPrecisionCrimeMatchProjection {
  val policeForceArea: PoliceForce
  val batchId: String
  val crimeReference: String
  val deviceSerialNumber: String
  val matchedAt: LocalDateTime
  val zeroPrecisionPositionCount: Long
}
