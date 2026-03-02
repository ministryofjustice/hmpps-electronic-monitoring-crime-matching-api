package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.IngestionStatus
import java.time.LocalDateTime

interface CrimeBatchIngestionAttemptSummaryProjection {
  val ingestionAttemptId: String
  val createdAt: LocalDateTime
  val batchId: String?
  val policeForceArea: String?
  val matches: Long?
  val ingestionStatus: IngestionStatus
}
