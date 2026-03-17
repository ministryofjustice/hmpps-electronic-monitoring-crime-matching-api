package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection

import java.time.LocalDateTime
import java.util.UUID

interface CrimeBatchIngestionAttemptProjection {
<<<<<<< Updated upstream
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
=======
  fun getIngestionAttemptId(): UUID
  fun getIngestionStatus(): String
  fun getPoliceForceArea(): String?
  fun getBatchId(): String?
  fun getCrimeBatchId(): UUID?
  fun getMatches(): Long?
  fun getCreatedAt(): LocalDateTime
  fun getFileName(): String?
  fun getSubmitted(): Int?
  fun getSuccessful(): Int?
  fun getFailed(): Int?
  fun getIsCrimeBatch(): Boolean
  fun getFailureSubCategory(): String?
>>>>>>> Stashed changes
}
