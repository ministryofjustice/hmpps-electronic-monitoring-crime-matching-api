package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.outbox

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.EmailOutbox
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface EmailOutboxRepository : JpaRepository<EmailOutbox, UUID> {

  /**
   * Transitions a row only when it is still in the expected source status. Returns the number of
   * rows updated so stale/duplicate workers can be detected without regressing terminal states.
   */
  @Modifying
  @Query(
    value = """
      UPDATE email_outbox
      SET status = :newStatus,
          attempts = attempts + 1,
          last_error = :lastError,
          updated_at = :updatedAt
      WHERE event_id = :eventId AND status = :expectedStatus
    """,
    nativeQuery = true,
  )
  fun transitionStatusIfCurrent(
    @Param("eventId") eventId: UUID,
    @Param("expectedStatus") expectedStatus: String,
    @Param("newStatus") newStatus: String,
    @Param("lastError") lastError: String?,
    @Param("updatedAt") updatedAt: LocalDateTime,
  ): Int

  /**
   * Claims a batch of eligible PENDING rows. `FOR UPDATE SKIP LOCKED` allows every
   * replica to poll concurrently without contending on or double-processing rows.
   * Returned entities are managed within the caller's transaction, so callers should
   * transition them to CLAIMED in the same transaction.
   */
  @Query(
    value = """
      SELECT * FROM email_outbox
      WHERE status = 'PENDING' AND available_at <= :now
      ORDER BY created_at
      LIMIT :limit
      FOR UPDATE SKIP LOCKED
    """,
    nativeQuery = true,
  )
  fun claimBatch(
    @Param("now") now: LocalDateTime,
    @Param("limit") limit: Int,
  ): List<EmailOutbox>

  /**
   * Returns leased-but-stale CLAIMED rows to PENDING so a crashed worker or failed
   * publish does not strand an event. Returns the number of rows reclaimed.
   */
  @Modifying
  @Query(
    value = """
      UPDATE email_outbox
      SET status = 'PENDING', claimed_at = NULL, claimed_by = NULL, updated_at = :now
      WHERE status = 'CLAIMED' AND claimed_at < :threshold
    """,
    nativeQuery = true,
  )
  fun reclaimExpired(
    @Param("now") now: LocalDateTime,
    @Param("threshold") threshold: LocalDateTime,
  ): Int
}
