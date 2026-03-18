package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeType
import java.time.Instant
import java.time.LocalDateTime

interface CrimeVersionProjection {
  val crimeReference: String
  val crimeType: CrimeType
  val crimeDateTimeFrom: Instant
  val crimeDateTimeTo: Instant
  val crimeText: String
  val deviceWearerId: String?
  val name: String?
  val deviceId: Long?
  val nomisId: String?
  val latitude: Double?
  val longitude: Double?
  val sequenceLabel: String?
  val confidence: Int?
  val capturedDateTime: LocalDateTime?
}
