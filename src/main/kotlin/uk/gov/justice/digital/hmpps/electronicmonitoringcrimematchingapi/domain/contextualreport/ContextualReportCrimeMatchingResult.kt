package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.domain.contextualreport

import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

data class ContextualReportCrimeMatchingResult(
  val batchId: String,
  val crimeReference: String,
  val crimeType: String,
  val matchingResultCreatedAt: Instant,
  val address: String,
  val dateOfBirth: LocalDateTime?,
  val identifier: String,
  val name: String,
  val pncRef: String,
  val deviceWearerId: UUID,
)
