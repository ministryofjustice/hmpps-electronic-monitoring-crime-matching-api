package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.outbox

import org.assertj.core.api.Assertions.assertThat
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.EmailData
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.NamedDataSource
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.EmailIngestionOutcome
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.EmailOutboxPayload
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.EmailOutbox
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeBatchEmailIngestionErrorType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.EmailOutboxStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.IngestionStatus
import java.time.LocalDateTime
import java.util.Date
import java.util.UUID

/**
 * Test fixtures and helpers for email outbox integration tests.
 */
object EmailOutboxTestFixtures {

  /**
   * Creates a minimal [EmailIngestionOutcome] for testing.
   */
  fun createTestEmailIngestionOutcome(
    crimeBatchId: String = UUID.randomUUID().toString(),
    ingestionStatus: IngestionStatus = IngestionStatus.SUCCESSFUL,
    sender: String = EmailOutboxTestConstants.TEST_SENDER,
    originalSender: String = EmailOutboxTestConstants.TEST_ORIGINAL_SENDER,
  ): EmailIngestionOutcome = EmailIngestionOutcome(
    crimeBatchId = crimeBatchId,
    emailData = EmailData(
      sender = sender,
      originalSender = originalSender,
      subject = EmailOutboxTestConstants.TEST_SUBJECT,
      sentAt = Date(),
      attachments = listOf(NamedDataSource("crimes.csv")),
    ),
    ingestionStatus = ingestionStatus,
    policeForce = EmailOutboxTestConstants.TEST_POLICE_FORCE,
    batchId = "BATCH-${UUID.randomUUID().toString().take(8).uppercase()}",
  )

  /**
   * Creates a test [EmailOutboxPayload] for verifying serialization roundtrips.
   */
  fun createTestEmailOutboxPayload(
    recipient: String = EmailOutboxTestConstants.TEST_SENDER,
    ingestionStatus: IngestionStatus = IngestionStatus.SUCCESSFUL,
    recordCount: Int = 1,
  ): EmailOutboxPayload = EmailOutboxPayload(
    schemaVersion = 1,
    ingestionStatus = ingestionStatus,
    recipient = recipient,
    fileName = "crimes_2025_08_14.csv",
    batchId = "BATCH-TEST",
    crimeBatchId = UUID.randomUUID().toString(),
    policeForce = EmailOutboxTestConstants.TEST_POLICE_FORCE,
    errorType = CrimeBatchEmailIngestionErrorType.UNKNOWN,
    recordCount = recordCount,
    records = emptyList(),
    errors = emptyList(),
  )

  /**
   * Creates a test [EmailOutbox] row in PENDING status.
   */
  fun createTestEmailOutboxRowPending(
    payload: String = "{}",
    crimeBatchId: UUID? = UUID.randomUUID(),
  ): EmailOutbox = EmailOutbox(
    eventId = UUID.randomUUID(),
    crimeBatchId = crimeBatchId,
    status = EmailOutboxStatus.PENDING,
    payload = payload,
    attempts = 0,
  )

  /**
   * Creates a test [EmailOutbox] row in CLAIMED status.
   */
  fun createTestEmailOutboxRowClaimed(
    payload: String = "{}",
    crimeBatchId: UUID? = UUID.randomUUID(),
  ): EmailOutbox = EmailOutbox(
    eventId = UUID.randomUUID(),
    crimeBatchId = crimeBatchId,
    status = EmailOutboxStatus.CLAIMED,
    payload = payload,
    attempts = 0,
    claimedAt = LocalDateTime.now(),
    claimedBy = "test-instance",
  )

  /**
   * Creates a test [EmailOutbox] row in SENT status.
   */
  fun createTestEmailOutboxRowSent(
    payload: String = "{}",
    crimeBatchId: UUID? = UUID.randomUUID(),
    attempts: Int = 1,
  ): EmailOutbox = EmailOutbox(
    eventId = UUID.randomUUID(),
    crimeBatchId = crimeBatchId,
    status = EmailOutboxStatus.SENT,
    payload = payload,
    attempts = attempts,
  )

  /**
   * Verifies that an [EmailOutbox] row is in the expected status with expected properties.
   */
  fun verifyOutboxRowStatus(
    row: EmailOutbox,
    expectedStatus: EmailOutboxStatus,
    expectedAttempts: Int? = null,
    expectedLastError: String? = null,
  ) {
    assertThat(row.status).isEqualTo(expectedStatus)
    if (expectedAttempts != null) {
      assertThat(row.attempts).isEqualTo(expectedAttempts)
    }
    if (expectedLastError != null) {
      assertThat(row.lastError).isEqualTo(expectedLastError)
    }
  }

  /**
   * Verifies that an [EmailOutbox] row is terminal (SENT, FAILED, or DEAD).
   */
  fun verifyOutboxRowIsTerminal(row: EmailOutbox) {
    assertThat(row.status).isIn(EmailOutboxStatus.SENT, EmailOutboxStatus.FAILED, EmailOutboxStatus.DEAD)
  }

  /**
   * Verifies that an [EmailOutbox] row has a CLAIMED lease set.
   */
  fun verifyOutboxRowIsClaimed(row: EmailOutbox) {
    assertThat(row.status).isEqualTo(EmailOutboxStatus.CLAIMED)
    assertThat(row.claimedAt).isNotNull()
    assertThat(row.claimedBy).isNotNull()
  }

  /**
   * Verifies that an [EmailOutboxPayload] contains all required fields for a given status.
   */
  fun verifyPayloadFidelity(
    payload: EmailOutboxPayload,
    expectedStatus: IngestionStatus,
  ) {
    // Common fields for all statuses
    assertThat(payload.schemaVersion).isEqualTo(1)
    assertThat(payload.ingestionStatus).isEqualTo(expectedStatus)
    assertThat(payload.recipient).isNotBlank()
    assertThat(payload.fileName).isNotBlank()
    assertThat(payload.batchId).isNotBlank()
    assertThat(payload.policeForce).isNotBlank()
    assertThat(payload.recordCount).isGreaterThanOrEqualTo(0)

    // Status-specific validations
    when (expectedStatus) {
      IngestionStatus.SUCCESSFUL -> {
        // SUCCESSFUL must have records list (even if empty in minimal test case)
        assertThat(payload.records).isNotNull()
      }
      IngestionStatus.PARTIAL, IngestionStatus.ERROR -> {
        // PARTIAL/ERROR should have errors
        assertThat(payload.errors).isNotNull()
      }
      IngestionStatus.FAILED, IngestionStatus.UNKNOWN -> {
        // FAILED/UNKNOWN have minimal payload
        assertThat(payload.recipient).isNotBlank()
      }
    }
  }
}
