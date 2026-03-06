package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection

import java.time.LocalDateTime

interface CrimeVersionSummaryProjection {
  val crimeVersionId: String
  val crimeBatchId: String
  val crimeReference: String
  val policeForceArea: String
  val crimeTypeId: String
  val crimeDateTimeFrom: LocalDateTime
  val batchId: String
  val ingestionDateTime: LocalDateTime
  val matched: Boolean
  val versionLabel: String
  val updates: String
}
