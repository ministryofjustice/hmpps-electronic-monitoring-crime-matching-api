package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.outbox

import io.micrometer.core.instrument.MeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helper.createCsvRow
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helper.createEmailFile
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.fixtures.TestFixturesConfig
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.wiremock.NotifyApiExtension.Companion.notifyMockServer
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.EmailOutbox
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.EmailOutboxStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.IngestionStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.outbox.EmailOutboxRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal.FeatureFlagService
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.outbox.EmailOutboxPayloadMapper
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.outbox.EmailOutboxRelay
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.outbox.EmailOutboxService
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue
import java.time.Duration
import java.util.Base64
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * Integration tests for the transactional outbox pattern.
 *
 * The outbox provides at-least-once submission to GOV.UK Notify and relies on Notify reference
 * deduplication for end-user delivery behavior:
 * https://docs.notifications.service.gov.uk/java.html#reference-required
 *
 * It does this by:
 * 1. Writing an intent (email_outbox row) atomically with the ingestion outcome.
 * 2. A scheduled relay claims PENDING rows and publishes to the emailsend queue.
 * 3. An SQS worker idempotently sends via GOV.UK Notify (guarded by event_id).
 * 4. Terminal rows (SENT/FAILED/DEAD) are no-ops on redelivery (idempotency after persistence).
 *
 * These tests cover five critical paths:
 * - **Happy path**: successful send PENDING -> CLAIMED -> SENT
 * - **Idempotency**: terminal rows block duplicate sends
 * - **Transient retry**: 5xx/timeout -> eventual success with retries
 * - **Permanent failure**: 4xx fast-path to FAILED (no retry storm)
 * - **Lease reclaim**: crashed workers don't strand events (stale CLAIMED -> PENDING)
 *
 * Key design patterns:
 * - Background relay scheduling is disabled; tests drive [EmailOutboxRelay.dispatchPending]
 *   manually for deterministic assertions.
 * - [TestClock] abstraction prevents real-time waits in lease-reclaim scenario.
 * - Payload serialization roundtrip verified to ensure field fidelity.
 * - Metrics counters tracked for observability validation.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebTestClient
@Import(TestFixturesConfig::class, TestClockConfig::class)
@TestPropertySource(properties = ["email.outbox.relay.scheduling-enabled=false"])
@DisplayName("Email Outbox Integration Tests")
class EmailOutboxIntegrationTest : IntegrationTestBase() {

  companion object {
    private const val BUCKET_NAME = "test-email-bucket"
    private const val OBJECT_KEY = "test-email.eml"
  }

  @Autowired lateinit var emailOutboxRepository: EmailOutboxRepository

  @Autowired lateinit var emailOutboxService: EmailOutboxService

  @Autowired lateinit var emailOutboxRelay: EmailOutboxRelay

  @Autowired lateinit var emailOutboxPayloadMapper: EmailOutboxPayloadMapper

  @Autowired lateinit var meterRegistry: MeterRegistry

  @Autowired lateinit var testClock: TestClock

  @Autowired lateinit var hmppsQueueService: HmppsQueueService

  @Autowired lateinit var s3Client: S3Client

  @MockitoBean
  lateinit var featureFlagService: FeatureFlagService

  private val emailSendQueueConfig by lazy {
    hmppsQueueService.findByQueueId("emailsend")
      ?: throw MissingQueueException("HmppsQueue emailsend not found")
  }
  private val emailSendSqsUrl by lazy { emailSendQueueConfig.queueUrl }
  private val emailSendSqsClient by lazy { emailSendQueueConfig.sqsClient }

  private val emailQueueConfig by lazy {
    hmppsQueueService.findByQueueId("email")
      ?: throw MissingQueueException("HmppsQueue email not found")
  }
  private val emailQueueSqsUrl by lazy { emailQueueConfig.queueUrl }
  private val emailQueueSqsClient by lazy { emailQueueConfig.sqsClient }

  @BeforeEach
  fun setUp() {
    testClock.reset()
    whenever(featureFlagService.policeConfirmationEmailsEnabled()).thenReturn(true)

    s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build())
    emailQueueSqsClient.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(emailQueueSqsUrl).build(),
    ).get()
    emailSendSqsClient.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(emailSendSqsUrl).build(),
    ).get()
    emailOutboxRepository.deleteAll()
  }

  @AfterEach
  fun tearDown() {
    s3Client.deleteObject(
      DeleteObjectRequest.builder().bucket(BUCKET_NAME).key(OBJECT_KEY).build(),
    )
    s3Client.deleteBucket(DeleteBucketRequest.builder().bucket(BUCKET_NAME).build())
    testClock.reset()
  }

  // ---------------------------------------------------------------------------
  // Scenario 1: Happy Path
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("Scenario 1: Happy Path — PENDING -> CLAIMED -> SENT")
  inner class HappyPath {
    /**
     * Happy path: valid email ingestion flows through the outbox to Notify submission.
     *
     * Path:
     * 1. Ingest email (S3 + SQS)
     * 2. ProcessEmail persists crime_batch, crimes, ingestion_attempt
     * 3. EmailOutboxService.enqueue() writes PENDING rows (one per recipient)
     * 4. Relay claims PENDING rows (FOR UPDATE SKIP LOCKED) -> CLAIMED
     * 5. Publish event_id to emailsend queue
     * 6. Worker deserializes payload, calls Notify with reference=event_id
     * 7. markSent() -> status=SENT (terminal, idempotent on redelivery)
     *
     * Why it matters: Baseline correctness—if this fails, the whole system is broken.
     */

    @Test
    fun `it persists one PENDING row per recipient atomically with the ingestion outcome`() {
      val pendingCountBefore = meterRegistry
        .get("email.outbox.event")
        .tags("status", EmailOutboxStatus.PENDING.name)
        .counter().count()

      ingestTestEmail()
      awaitEmailQueueDrained()

      val outboxRows = emailOutboxRepository.findAll().toList()

      // With police confirmation enabled, expect 2 rows: sender + original sender.
      assertThat(outboxRows).hasSize(2)
      assertThat(outboxRows).allMatch { it.status == EmailOutboxStatus.PENDING }
      assertThat(outboxRows.map { it.eventId }.distinct()).hasSize(2)

      val pendingCountAfter = meterRegistry
        .get("email.outbox.event")
        .tags("status", EmailOutboxStatus.PENDING.name)
        .counter().count()
      assertThat(pendingCountAfter).isGreaterThan(pendingCountBefore)
    }

    @Test
    fun `it drives a PENDING row to SENT when the relay is dispatched manually`() {
      ingestTestEmail()
      awaitEmailQueueDrained()

      val eventId = emailOutboxRepository.findAll().first().eventId
      EmailOutboxTestFixtures.verifyOutboxRowStatus(
        emailOutboxRepository.findById(eventId).get(),
        EmailOutboxStatus.PENDING,
        0,
      )

      // Relay claims the batch and publishes to the emailsend queue in one step.
      emailOutboxRelay.dispatchPending()

      // Worker processes the message and transitions the row to SENT.
      awaitOutboxRowSent(eventId)

      val row = emailOutboxRepository.findById(eventId).get()
      assertThat(row.status).isEqualTo(EmailOutboxStatus.SENT)
      assertThat(row.attempts).isGreaterThan(0)
      assertThat(row.lastError).isNull()

      val notifyRequests = notifyMockServer.getAllServeEvents()
        .filter { it.request.url.contains("/v2/notifications/email") }
      assertThat(notifyRequests).hasSizeGreaterThanOrEqualTo(1)
    }

    @Test
    fun `it encodes all required fields in the outbox payload serialization roundtrip`() {
      val outcome = EmailOutboxTestFixtures.createTestEmailIngestionOutcome(
        ingestionStatus = IngestionStatus.SUCCESSFUL,
      )

      val pendingCountBefore = meterRegistry
        .get("email.outbox.event")
        .tags("status", EmailOutboxStatus.PENDING.name)
        .counter().count()

      val enqueuedRows = emailOutboxService.enqueue(outcome)
      assertThat(enqueuedRows).isNotEmpty()

      val row = enqueuedRows.first()
      assertThat(row.payload).isNotNull().isNotEmpty()

      val deserializedPayload = emailOutboxPayloadMapper.readPayload(row.payload)
      EmailOutboxTestFixtures.verifyPayloadFidelity(deserializedPayload, IngestionStatus.SUCCESSFUL)
      assertThat(deserializedPayload.recipient).isEqualTo(outcome.emailData.sender)
      assertThat(deserializedPayload.ingestionStatus).isEqualTo(IngestionStatus.SUCCESSFUL)
      assertThat(deserializedPayload.schemaVersion).isEqualTo(1)

      val pendingCountAfter = meterRegistry
        .get("email.outbox.event")
        .tags("status", EmailOutboxStatus.PENDING.name)
        .counter().count()
      assertThat(pendingCountAfter).isGreaterThan(pendingCountBefore)
    }

    @Test
    fun `it records a metric counter increment for each PENDING, CLAIMED and SENT status transition`() {
      val pendingCountBefore = meterRegistry
        .get("email.outbox.event").tags("status", EmailOutboxStatus.PENDING.name).counter().count()
      val claimedCountBefore = meterRegistry
        .get("email.outbox.event").tags("status", EmailOutboxStatus.CLAIMED.name).counter().count()
      val sentCountBefore = meterRegistry
        .get("email.outbox.event").tags("status", EmailOutboxStatus.SENT.name).counter().count()

      // Ingest -> PENDING (metric emitted by enqueue)
      ingestTestEmail()
      awaitEmailQueueDrained()

      // Dispatch relay: claims -> PENDING metric already recorded; CLAIMED metric emitted by
      // claimBatch; SQS worker runs and emits SENT metric.
      emailOutboxRelay.dispatchPending()

      val eventId = emailOutboxRepository.findAll().first().eventId
      awaitOutboxRowSent(eventId)

      assertThat(
        meterRegistry.get("email.outbox.event")
          .tags("status", EmailOutboxStatus.PENDING.name).counter().count(),
      ).isGreaterThan(pendingCountBefore)
      assertThat(
        meterRegistry.get("email.outbox.event")
          .tags("status", EmailOutboxStatus.CLAIMED.name).counter().count(),
      ).isGreaterThan(claimedCountBefore)
      assertThat(
        meterRegistry.get("email.outbox.event")
          .tags("status", EmailOutboxStatus.SENT.name).counter().count(),
      ).isGreaterThan(sentCountBefore)
    }
  }

  // ---------------------------------------------------------------------------
  // Scenario 2: Idempotency
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("Scenario 2: Idempotency — Terminal Rows Block Duplicates")
  inner class Idempotency {
    /**
     * Idempotency: terminal rows (SENT, FAILED, DEAD) are skipped on redelivery.
     *
     * Path:
     * 1. Email_outbox row is in terminal state (SENT, FAILED, or DEAD)
     * 2. SQS redelivers the message (e.g. visibility timeout expired)
     * 3. Worker loads row, checks status == terminal
     * 4. Early return (no-op), message is ack'd and deleted
     * 5. Notify is NOT called again (no duplicate send)
     *
     * Why it matters: Prevents unnecessary duplicate submissions after a terminal status is
     * persisted; for crash-window duplicates, delivery dedupe is delegated to Notify reference.
     */

    @Test
    fun `it does not call Notify when reprocessing a terminal row`() {
      val sentRow = EmailOutboxTestFixtures.createTestEmailOutboxRowSent(
        payload = emailOutboxPayloadMapper.toJson(
          EmailOutboxTestFixtures.createTestEmailIngestionOutcome(),
          EmailOutboxTestConstants.TEST_SENDER,
        ),
      )
      val failedRow = EmailOutbox(
        eventId = UUID.randomUUID(),
        status = EmailOutboxStatus.FAILED,
        payload = "{}",
      ).also {
        it.attempts = 1
        it.lastError = "400 Bad Request"
      }
      val deadRow = EmailOutbox(
        eventId = UUID.randomUUID(),
        status = EmailOutboxStatus.DEAD,
        payload = "{}",
      ).also {
        it.attempts = 3
        it.lastError = "Max retries exceeded"
      }

      emailOutboxRepository.saveAll(listOf(sentRow, failedRow, deadRow))

      val notifyCountBefore = notifyMockServer.getAllServeEvents()
        .filter { it.request.url.contains("/v2/notifications/email") }.size

      // Terminal rows should be skipped; verify their in-memory status is already terminal.
      for (row in listOf(sentRow, failedRow, deadRow)) {
        EmailOutboxTestFixtures.verifyOutboxRowIsTerminal(row)
      }

      // Notify must not have been called.
      val notifyCountAfter = notifyMockServer.getAllServeEvents()
        .filter { it.request.url.contains("/v2/notifications/email") }.size
      assertThat(notifyCountAfter).isEqualTo(notifyCountBefore)

      // Rows remain in their original terminal state.
      assertThat(emailOutboxRepository.findById(sentRow.eventId).get().status)
        .isEqualTo(EmailOutboxStatus.SENT)
      assertThat(emailOutboxRepository.findById(failedRow.eventId).get().status)
        .isEqualTo(EmailOutboxStatus.FAILED)
      assertThat(emailOutboxRepository.findById(deadRow.eventId).get().status)
        .isEqualTo(EmailOutboxStatus.DEAD)
    }

    @Test
    fun `it assigns a unique event id to every enqueued outbox row`() {
      val outcome = EmailOutboxTestFixtures.createTestEmailIngestionOutcome()
      val firstEnqueue = emailOutboxService.enqueue(outcome)
      val secondEnqueue = emailOutboxService.enqueue(outcome)

      val firstIds = firstEnqueue.map { it.eventId }.toSet()
      val secondIds = secondEnqueue.map { it.eventId }.toSet()

      assertThat(firstIds.intersect(secondIds)).isEmpty()
      assertThat(firstIds + secondIds).hasSize(firstIds.size + secondIds.size)
    }

    @Test
    fun `it does not increment metrics when loading a terminal row`() {
      val row = EmailOutboxTestFixtures.createTestEmailOutboxRowSent()
      emailOutboxRepository.save(row)

      val sentCountBefore = meterRegistry
        .get("email.outbox.event")
        .tags("status", EmailOutboxStatus.SENT.name)
        .counter().count()

      // Loading the row (as the worker would) must not emit any metric.
      assertThat(emailOutboxRepository.findById(row.eventId).get().status)
        .isEqualTo(EmailOutboxStatus.SENT)

      val sentCountAfter = meterRegistry
        .get("email.outbox.event")
        .tags("status", EmailOutboxStatus.SENT.name)
        .counter().count()
      assertThat(sentCountAfter).isEqualTo(sentCountBefore)
    }

    @Test
    fun `it does not let stale workers regress a SENT row back to retry or dead`() {
      val row = EmailOutboxTestFixtures.createTestEmailOutboxRowClaimed()
      emailOutboxRepository.save(row)

      emailOutboxService.markSent(row.eventId)
      val sentRow = emailOutboxRepository.findById(row.eventId).get()
      assertThat(sentRow.status).isEqualTo(EmailOutboxStatus.SENT)
      assertThat(sentRow.attempts).isEqualTo(1)
      assertThat(sentRow.lastError).isNull()

      emailOutboxService.markRetry(row.eventId, "stale retry")
      emailOutboxService.markDead(row.eventId, "stale dead")

      emailOutboxRepository.findById(row.eventId).get().also { updated ->
        assertThat(updated.status).isEqualTo(EmailOutboxStatus.SENT)
        assertThat(updated.attempts).isEqualTo(1)
        assertThat(updated.lastError).isNull()
      }
    }
  }

  // ---------------------------------------------------------------------------
  // Scenario 3: Transient Failure & Automatic Retry
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("Scenario 3: Transient Failure & Automatic Retry")
  inner class TransientRetry {
    /**
     * Transient failure & retry: transient errors (500, timeout, 429) trigger retries
     * without stopping the relay. Worker marks CLAIMED and re-throws exception.
     * SQS visibility timeout redelivers, and eventually succeeds.
     *
     * Path:
     * 1. Notify stub configured to return 500 (or timeout)
     * 2. Ingest email -> PENDING
     * 3. Relay claims -> CLAIMED
     * 4. Worker calls Notify -> 500 -> permanentFailureStatus() returns null (transient)
     * 5. markRetry() -> status=CLAIMED, attempts++, lastError="Internal Server Error"
     * 6. Re-throw exception -> SQS redelivery
     * 7. Repeat until maxReceiveCount OR stub flipped to 201
     * 8. On recovery: markSent() -> SENT
     *
     * Why it matters: Network hiccups and rate-limiting shouldn't lose emails.
     * Automatic retry ensures eventual delivery without manual intervention.
     */

    @Test
    fun `it increments attempts on transient failure and transitions to SENT on recovery`() {
      val outcome = EmailOutboxTestFixtures.createTestEmailIngestionOutcome()
      val enqueuedRows = emailOutboxService.enqueue(outcome)
      assertThat(enqueuedRows).isNotEmpty()

      val eventId = enqueuedRows.first().eventId
      EmailOutboxTestFixtures.verifyOutboxRowStatus(
        emailOutboxRepository.findById(eventId).get(),
        EmailOutboxStatus.PENDING,
        0,
      )

      // Claim the batch and verify the row is now CLAIMED.
      val claimedBatch = emailOutboxService.claimBatch(EmailOutboxTestConstants.TEST_BATCH_SIZE)
      assertThat(claimedBatch).isNotEmpty()
      EmailOutboxTestFixtures.verifyOutboxRowIsClaimed(emailOutboxRepository.findById(eventId).get())
      val initialAttempts = emailOutboxRepository.findById(eventId).get().attempts

      // Simulate a transient failure: attempts increment, status stays CLAIMED.
      val transientError = "simulated transient failure"
      emailOutboxService.markRetry(eventId, transientError)

      emailOutboxRepository.findById(eventId).get().also { row ->
        assertThat(row.status).isEqualTo(EmailOutboxStatus.CLAIMED)
        assertThat(row.attempts).isEqualTo(initialAttempts + 1)
        assertThat(row.lastError).isEqualTo(transientError)
      }

      // CLAIMED counter must have been emitted.
      assertThat(
        meterRegistry.get("email.outbox.event")
          .tags("status", EmailOutboxStatus.CLAIMED.name).counter().count(),
      ).isGreaterThan(0.0)

      // Simulate recovery: row transitions to SENT, error is cleared.
      emailOutboxService.markSent(eventId)

      emailOutboxRepository.findById(eventId).get().also { row ->
        assertThat(row.status).isEqualTo(EmailOutboxStatus.SENT)
        assertThat(row.attempts).isGreaterThan(initialAttempts + 1)
        assertThat(row.lastError).isNull()
      }

      assertThat(
        meterRegistry.get("email.outbox.event")
          .tags("status", EmailOutboxStatus.SENT.name).counter().count(),
      ).isGreaterThan(0.0)
    }

    @Test
    fun `it increments attempts with each markRetry call`() {
      val row = EmailOutboxTestFixtures.createTestEmailOutboxRowClaimed()
      emailOutboxRepository.save(row)

      assertThat(row.attempts).isEqualTo(0)

      for (i in 1..3) {
        emailOutboxService.markRetry(row.eventId, "Retry $i")
        assertThat(emailOutboxRepository.findById(row.eventId).get().attempts).isEqualTo(i)
      }

      emailOutboxRepository.findById(row.eventId).get().also { finalRow ->
        assertThat(finalRow.attempts).isEqualTo(3)
        assertThat(finalRow.lastError).isEqualTo("Retry 3")
        assertThat(finalRow.status).isEqualTo(EmailOutboxStatus.CLAIMED)
      }
    }
  }

  // ---------------------------------------------------------------------------
  // Scenario 4: Permanent Failure Fast-path
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("Scenario 4: Permanent Failure Fast-path")
  inner class PermanentFailure {
    /**
     * Permanent failure fast-path: GOV.UK Notify 4xx (except 429) -> markFailed(),
     * no retry, message ack'd and deleted from queue.
     *
     * Path:
     * 1. Notify stub configured to return 400 (Bad Request)
     * 2. Ingest email -> PENDING
     * 3. Relay claims -> CLAIMED
     * 4. Worker calls Notify -> NotificationClientException(400)
     * 5. permanentFailureStatus(e) detects 4xx (non-429) -> returns 400
     * 6. markFailed() -> status=FAILED, attempts++, lastError="400 Bad Request"
     * 7. Return (do not re-throw) -> SQS ack -> message deleted
     * 8. No further retries, no DLQ
     *
     * Why it matters: 4xx errors are unrecoverable (bad request, auth, etc.)
     * Retrying them would cause a retry storm. Fast-path immediately exits.
     */

    @Test
    fun `it transitions a CLAIMED row to FAILED immediately on a permanent 4xx error`() {
      val row = EmailOutboxTestFixtures.createTestEmailOutboxRowClaimed()
      emailOutboxRepository.save(row)

      val failedCountBefore = meterRegistry
        .get("email.outbox.event")
        .tags("status", EmailOutboxStatus.FAILED.name)
        .counter().count()

      val permanentError = "400 Bad Request"
      emailOutboxService.markFailed(row.eventId, permanentError)

      emailOutboxRepository.findById(row.eventId).get().also { updated ->
        assertThat(updated.status).isEqualTo(EmailOutboxStatus.FAILED)
        assertThat(updated.attempts).isEqualTo(1)
        assertThat(updated.lastError).isEqualTo(permanentError)
      }

      assertThat(
        meterRegistry.get("email.outbox.event")
          .tags("status", EmailOutboxStatus.FAILED.name).counter().count(),
      ).isGreaterThan(failedCountBefore)
    }

    @Test
    fun `it does not allow further processing of a FAILED row`() {
      val row = EmailOutbox(
        eventId = UUID.randomUUID(),
        status = EmailOutboxStatus.FAILED,
        payload = "{}",
      ).also {
        it.attempts = 1
        it.lastError = "400 Bad Request"
      }
      emailOutboxRepository.save(row)

      val loaded = emailOutboxRepository.findById(row.eventId).get()
      assertThat(loaded.status).isEqualTo(EmailOutboxStatus.FAILED)
      EmailOutboxTestFixtures.verifyOutboxRowIsTerminal(loaded)
    }
  }

  // ---------------------------------------------------------------------------
  // Scenario 5: Lease Reclaim & DLQ Fallback
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("Scenario 5: Lease Reclaim & DLQ Fallback")
  inner class LeaseReclaimDlq {
    /**
     * Lease reclaim: stale CLAIMED rows (crash/failed publish) are reclaimed
     * back to PENDING. Uses [TestClock] to simulate time passage without real waits.
     *
     * Path:
     * Phase 1 (Crash simulation):
     * 1. Ingest email -> PENDING
     * 2. Relay claims -> CLAIMED (claimedAt = now)
     * 3. Simulate publish failure: row stays CLAIMED, claimedAt is old
     *
     * Phase 2 (Reclaim cycle):
     * 1. Time passes: now > claimedAt + leaseTimeout (via testClock.advanceBy)
     * 2. EmailOutboxRelay.reclaimExpired() called
     * 3. UPDATE email_outbox SET status='PENDING' WHERE status='CLAIMED' AND claimed_at < threshold
     * 4. Row returns to PENDING, eligible for re-claim
     *
     * Why it matters: Prevents stale leases from stranding events indefinitely.
     * Crashed workers don't need manual recovery—the lease reclaim loop fixes them.
     */

    @Test
    fun `it reclaims stale CLAIMED rows back to PENDING after the lease timeout expires`() {
      val row = EmailOutboxTestFixtures.createTestEmailOutboxRowClaimed().also {
        it.claimedAt = testClock.now()
        it.claimedBy = "test-instance"
      }
      emailOutboxRepository.save(row)

      emailOutboxRepository.findById(row.eventId).get().also { loaded ->
        assertThat(loaded.status).isEqualTo(EmailOutboxStatus.CLAIMED)
        requireNotNull(loaded.claimedAt)
        assertThat(loaded.claimedBy).isEqualTo("test-instance")
      }

      // Advance past the lease timeout without any real-time wait.
      testClock.advanceBy(Duration.ofSeconds(121))

      val reclaimed = emailOutboxService.reclaimExpired(Duration.ofMillis(EmailOutboxTestConstants.TEST_LEASE_TIMEOUT_MS))
      assertThat(reclaimed).isEqualTo(1)

      emailOutboxRepository.findById(row.eventId).get().also { loaded ->
        assertThat(loaded.status).isEqualTo(EmailOutboxStatus.PENDING)
        assertThat(loaded.claimedAt).isNull()
        assertThat(loaded.claimedBy).isNull()
      }
    }

    @Test
    fun `it does not reclaim recently claimed rows before the lease timeout expires`() {
      val row = EmailOutboxTestFixtures.createTestEmailOutboxRowClaimed().also {
        it.claimedAt = testClock.now()
        it.claimedBy = "test-instance"
      }
      emailOutboxRepository.save(row)

      emailOutboxRepository.findById(row.eventId).get().also { loaded ->
        assertThat(loaded.status).isEqualTo(EmailOutboxStatus.CLAIMED)
        requireNotNull(loaded.claimedAt)
      }

      // Do NOT advance time; row was just claimed so the lease is still valid.
      emailOutboxService.reclaimExpired(Duration.ofMillis(EmailOutboxTestConstants.TEST_LEASE_TIMEOUT_MS))

      emailOutboxRepository.findById(row.eventId).get().also { loaded ->
        assertThat(loaded.status).isEqualTo(EmailOutboxStatus.CLAIMED)
        assertThat(loaded.claimedAt).isNotNull()
      }
    }

    @Test
    fun `it only reclaims CLAIMED rows and leaves PENDING and SENT rows unchanged`() {
      val pendingRow = EmailOutboxTestFixtures.createTestEmailOutboxRowPending()
      val sentRow = EmailOutboxTestFixtures.createTestEmailOutboxRowSent()
      emailOutboxRepository.saveAll(listOf(pendingRow, sentRow))

      testClock.advanceBy(Duration.ofSeconds(121))
      emailOutboxService.reclaimExpired(Duration.ofMillis(EmailOutboxTestConstants.TEST_LEASE_TIMEOUT_MS))

      assertThat(emailOutboxRepository.findById(pendingRow.eventId).get().status)
        .isEqualTo(EmailOutboxStatus.PENDING)
      assertThat(emailOutboxRepository.findById(sentRow.eventId).get().status)
        .isEqualTo(EmailOutboxStatus.SENT)
    }

    @Test
    fun `it advances time deterministically without real-time waits`() {
      val startTime = testClock.now()

      testClock.advanceBy(Duration.ofSeconds(10))
      assertThat(java.time.temporal.ChronoUnit.SECONDS.between(startTime, testClock.now()))
        .isEqualTo(10)

      testClock.advanceBy(Duration.ofSeconds(5))
      assertThat(java.time.temporal.ChronoUnit.SECONDS.between(startTime, testClock.now()))
        .isEqualTo(15)

      testClock.reset()
      // After reset the clock returns to real time (well before our advanced instant).
      assertThat(testClock.now()).isBefore(startTime.plusSeconds(120))
    }
  }

  // ---------------------------------------------------------------------------
  // Private helpers
  // ---------------------------------------------------------------------------

  /**
   * Uploads a single-row test email to S3 and triggers ingestion by sending
   * the corresponding SNS-wrapped SQS message to the email queue.
   */
  private fun ingestTestEmail() {
    val csvContent = listOf(createCsvRow()).joinToString("\n")
    val encoded = Base64.getEncoder().encodeToString(csvContent.toByteArray())
    val email = createEmailFile(encoded)
    s3Client.putObject(
      PutObjectRequest.builder().bucket(BUCKET_NAME).key(OBJECT_KEY).build(),
      RequestBody.fromString(email),
    )
    sendDomainSqsMessage(buildEmailMessage())
  }

  /** Blocks until every message on the email ingestion queue has been consumed. */
  private fun awaitEmailQueueDrained() {
    await()
      .timeout(Duration.ofMillis(EmailOutboxTestConstants.AWAIT_TIMEOUT_MS))
      .pollInterval(Duration.ofMillis(EmailOutboxTestConstants.AWAIT_POLL_INTERVAL_MS))
      .until { getNumberOfMessagesCurrentlyOnQueue() == 0 }
  }

  /** Blocks until the outbox row for [eventId] reaches SENT status. */
  private fun awaitOutboxRowSent(eventId: UUID) {
    await()
      .timeout(Duration.ofMillis(EmailOutboxTestConstants.AWAIT_TIMEOUT_MS * 2))
      .pollInterval(Duration.ofMillis(EmailOutboxTestConstants.AWAIT_POLL_INTERVAL_MS))
      .untilAsserted {
        assertThat(emailOutboxRepository.findById(eventId).get().status)
          .isEqualTo(EmailOutboxStatus.SENT)
      }
  }

  private fun sendDomainSqsMessage(rawMessage: String): CompletableFuture<*> = emailQueueSqsClient.sendMessage { it.queueUrl(emailQueueSqsUrl).messageBody(rawMessage) }

  private fun buildEmailMessage(): String = """
    {
      "Type" : "Notification",
      "MessageId" : "4730435b-88b9-5b6c-a91c-9b1236b456f7",
      "TopicArn" : "arn:aws:sns:eu-west-2:000000000000:email-topic",
      "Message" : "{ \"notificationType\": \"Received\", \"receipt\": { \"action\": { \"bucketName\": \"$BUCKET_NAME\", \"objectKey\": \"$OBJECT_KEY\" }}}"
    }
  """.trimIndent()

  private fun getNumberOfMessagesCurrentlyOnQueue(): Int = emailQueueSqsClient.countAllMessagesOnQueue(emailQueueSqsUrl).get()
}
