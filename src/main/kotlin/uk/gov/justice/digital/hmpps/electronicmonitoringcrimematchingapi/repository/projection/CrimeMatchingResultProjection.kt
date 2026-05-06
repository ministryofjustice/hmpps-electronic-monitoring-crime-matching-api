package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection

import java.time.Instant
import java.time.LocalDateTime

interface CrimeMatchingResultProjection {
  val policeForceArea: String
  val batchId: String
  val crimeReference: String
  val crimeTypeId: String
  val crimeDateTimeFrom: Instant
  val crimeDateTimeTo: Instant
  val crimeLatitude: Double?
  val crimeLongitude: Double?
  val crimeEasting: Double?
  val crimeNorthing: Double?
  val crimeText: String
  val address: String
  val dateOfBirth: LocalDateTime
  val deviceId: Long
  val identifier: String
  val name: String
  val nomisId: String
  val pncRef: String
}
