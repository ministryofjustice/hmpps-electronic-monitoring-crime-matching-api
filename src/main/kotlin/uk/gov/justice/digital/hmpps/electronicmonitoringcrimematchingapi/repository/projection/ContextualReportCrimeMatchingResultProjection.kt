package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection

import java.time.LocalDateTime

interface ContextualReportCrimeMatchingResultProjection {
  val batchId: String
  val crimeReference: String
  val crimeTypeId: String
  val matchingResultCreatedAt: LocalDateTime
  val address: String
  val dateOfBirth: LocalDateTime?
  val identifier: String
  val name: String
  val pncRef: String
  val deviceId: Long
}
