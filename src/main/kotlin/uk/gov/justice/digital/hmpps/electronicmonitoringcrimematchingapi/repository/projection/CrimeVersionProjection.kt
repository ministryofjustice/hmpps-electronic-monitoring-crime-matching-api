package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeType
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

interface CrimeVersionProjection {
  val crimeVersionId: UUID
  val crimeReference: String
  val crimeType: CrimeType
  val crimeDateTimeFrom: Instant
  val crimeDateTimeTo: Instant
  val crimeText: String
  val crimeLatitude: Double?
  val crimeLongitude: Double?
  val crimeNorthing: Double?
  val crimeEasting: Double?
  val matchingResultId: String?
  val deviceWearerId: String?
  val name: String?
  val deviceId: Long?
  val nomisId: String?
  val wearerLatitude: Double?
  val wearerLongitude: Double?
  val sequenceLabel: String?
  val confidence: Int?
  val capturedDateTime: LocalDateTime?
}
