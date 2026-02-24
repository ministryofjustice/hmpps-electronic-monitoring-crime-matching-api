package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection

import java.time.LocalDateTime

interface CrimeMatchingResultProjection {
  val policeForceArea: String
  val batchId: String
  val crimeReference: String
  val crimeTypeId: String
  val crimeDateTimeFrom: LocalDateTime
  val crimeDateTimeTo: LocalDateTime
  val crimeLatitude: Double?
  val crimeLongitude: Double?
  val crimeEasting: Double?
  val crimeNorthing: Double?
  val crimeText: String
  val deviceId: Long
  val name: String
  val nomisId: String
}
