package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection

import java.time.Instant
import java.time.LocalDateTime

interface CrimeVersionSummaryProjection {
  val crimeVersionId: String
  val crimeReference: String
  val policeForceArea: String
  val crimeTypeId: String
  val crimeDateTimeFrom: Instant
  val batchId: String
  val ingestionDateTime: LocalDateTime
  val matched: Boolean
  val versionLabel: String
  val updates: String
}
