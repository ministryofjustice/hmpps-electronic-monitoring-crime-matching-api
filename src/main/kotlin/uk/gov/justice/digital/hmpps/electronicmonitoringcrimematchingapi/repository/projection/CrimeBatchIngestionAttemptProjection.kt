package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection

import java.time.LocalDateTime

interface CrimeBatchIngestionAttemptProjection {
  val ingestionAttemptId: String
  val ingestionStatus: IngestionStatus
  val policeForceArea: String?
  val batchId: String?
  val matches: Long?
  val createdAt: LocalDateTime
  val fileName: String?
  val submitted: Int?
  val successful: Int?
  val failed: Int?
}
