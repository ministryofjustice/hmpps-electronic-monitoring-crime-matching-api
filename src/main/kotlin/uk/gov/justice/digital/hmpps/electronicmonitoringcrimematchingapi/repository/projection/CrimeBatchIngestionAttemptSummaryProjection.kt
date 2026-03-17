package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection

import java.time.LocalDateTime
import java.util.UUID

interface CrimeBatchIngestionAttemptSummaryProjection {
  fun getIngestionAttemptId(): UUID
  fun getCreatedAt(): LocalDateTime
  fun getCrimeBatchId(): UUID?
  fun getBatchId(): String?
  fun getPoliceForceArea(): String?
  fun getMatches(): Long?
  fun getIngestionStatus(): String
}
