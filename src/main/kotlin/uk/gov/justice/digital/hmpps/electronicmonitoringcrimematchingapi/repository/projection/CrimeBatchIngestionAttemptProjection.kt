package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.IngestionStatus
import java.time.LocalDateTime

interface CrimeBatchIngestionAttemptProjection {
  val ingestionAttemptId: String
  val ingestionStatus: IngestionStatus
  val policeForceArea: String?
  val batchId: String?
  val matches: Long?
  val createdAt: LocalDateTime
  val fileName: String?
}
