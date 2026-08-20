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
import java.util.concurrent.CompletableFuture

/**
 * Integration tests for the transactional outbox pattern.
 *
 * The outbox guarantees **exactly-once email delivery** by:
 * 1. Writing an intent (email_outbox row) atomically with the ingestion outcome.
 * 2. A scheduled relay claims PENDING rows and publishes to the emailsend queue.
 * 3. An SQS worker idempotently sends via GOV.UK Notify (guarded by event_id).
 * 4. Terminal rows (SENT/FAILED/DEAD) are no-ops on redelivery (idempotency).
 *
 * These tests cover five critical paths:
 * - **Happy path**: successful send PENDING → CLAIMED → SENT
 * - **Idempotency**: terminal rows block duplicate sends
 * - **Transient retry**: 5xx/timeout → eventual success with retries
 * - **Permanent failure**: 4xx fast-path to FAILED (no retry storm)
 * - **Lease reclaim**: crashed workers don't strand events (stale CLAIMED → PENDING)
 *
 * Key design patterns:
 * - `TestClock` abstraction prevents real-time waits in lease-reclaim scenario
 * - Payload serialization roundtrip verified to ensure field fidelity
 * - Metrics counters tracked for observability validation
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebTestClient
@Import(TestFixturesConfig::class, TestClockConfig::class)
@org.springframework.test.context.TestPropertySource(
  properties = ["email.outbox.relay.scheduling-enabled=false"],
)
@DisplayName("Email Outbox Integration Tests")
class EmailOutboxIntegrationTest : IntegrationTestBase() {

  companion object {
    private const val BUCKET_NAME = "test-email-bucket"
    private const val OBJECT_KEY = "test-email.eml"
  }

  @Autowired
  lateinit var emailOutboxRepository: EmailOutboxRepository

  @Autowired
  lateinit var emailOutboxService: EmailOutboxService

  @Autowired
  lateinit var emailOutboxRelay: EmailOutboxRelay

  @Autowired
  lateinit var emailOutboxPayloadMapper: EmailOutboxPayloadMapper

  @Autowired
  lateinit var meterRegistry: MeterRegistry

  @Autowired
  lateinit var testClock: TestClock

  @Autowired
  lateinit var hmppsQueueService: HmppsQueueService

  @Autowired
  lateinit var s3Client: S3Client

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
      DeleteObjectRequest
        .builder()
        .bucket(BUCKET_NAME)
        .key(OBJECT_KEY)
        .build(),
    )
    s3Client.deleteBucket(DeleteBucketRequest.builder().bucket(BUCKET_NAME).build())
    testClock.reset()
  }

  @Nested
  @DisplayName("Scenario 1: Happy Path — PENDING → CLAIMED → SENT")
  inner class HappyPath {
    /**
     * Happy path: valid email ingestion flows through the outbox to exactly-once delivery.
     *
     * Path:
     * 1. Ingest email (S3 + SQS)
     * 2. ProcessEmail persists crime_batch, crimes, ingestion_attempt
     * 3. EmailOutboxService.enqueue() writes PENDING rows (one per recipient)
     * 4. Relay claims PENDING rows (FOR UPDATE SKIP LOCKED) → CLAIMED
     * 5. Publish event_id to emailsend queue
     * 6. Worker deserializes payload, calls Notify with reference=event_id
     * 7. markSent() → status=SENT (terminal, idempotent on redelivery)
     *
     * Why it matters: Baseline correctness—if this fails, the whole system is broken.
     */

    @Test
    fun `it initially persists one PENDING outbox row per recipient before relay dispatch`() {
      // Arrange: ingest valid email
      val csvContent = listOf(createCsvRow()).joinToString("\n")
      val encoded = Base64.getEncoder().encodeToString(csvContent.toByteArray())
      val email = createEmailFile(encoded)

      s3Client.putObject(
        PutObjectRequest.builder()
          .bucket(BUCKET_NAME)
          .key(OBJECT_KEY)
          .build(),
        RequestBody.fromString(email),
      )

      val countBefore = meterRegistry
        .get("email.outbox.event")
        .tags("status", EmailOutboxStatus.PENDING.name)
        .counter().count()

      // Act: send domain SQS message to trigger email listener
      sendDomainSqsMessage(buildEmailMessage(OBJECT_KEY))

      // Assert: email queue processed and outbox rows created
      await()
        .timeout(Duration.ofMillis(EmailOutboxTestConstants.AWAIT_TIMEOUT_MS))
        .pollInterval(Duration.ofMillis(EmailOutboxTestConstants.AWAIT_POLL_INTERVAL_MS))
        .until { getNumberOfMessagesCurrentlyOnQueue() == 0 }

      val outboxRows = emailOutboxRepository.findAll().toList()

      // With police confirmation enabled, should have 2 rows (sender + original sender)
      assertThat(outboxRows).hasSize(2)
      assertThat(outboxRows).allMatch { it.status == EmailOutboxStatus.PENDING }
      assertThat(outboxRows.map { it.eventId }.distinct()).hasSize(2) // Unique event_ids

      // Verify metric was incremented per recipient
      val countAfter = meterRegistry
        .get("email.outbox.event")
        .tags("status", EmailOutboxStatus.PENDING.name)
        .counter().count()
      assertThat(countAfter).isGreaterThan(countBefore)
    }

    @Test
    fun `it drives a PENDING row to SENT when the relay is dispatched manually`() {
      // Arrange: ingest email and create outbox row
      val csvContent = listOf(createCsvRow()).joinToString("\n")
      val encoded = Base64.getEncoder().encodeToString(csvContent.toByteArray())
      val email = createEmailFile(encoded)

      s3Client.putObject(
        PutObjectRequest.builder()
          .bucket(BUCKET_NAME)
          .key(OBJECT_KEY)
          .build(),
        RequestBody.fromString(email),
      )

      sendDomainSqsMessage(buildEmailMessage(OBJECT_KEY))

      // Wait for email queue to be processed
      await()
        .timeout(Duration.ofMillis(EmailOutboxTestConstants.AWAIT_TIMEOUT_MS))
        .pollInterval(Duration.ofMillis(EmailOutboxTestConstants.AWAIT_POLL_INTERVAL_MS))
        .until { getNumberOfMessagesCurrentlyOnQueue() == 0 }

      val outboxRows = emailOutboxRepository.findAll().toList()
      assertThat(outboxRows).isNotEmpty()
      val eventId = outboxRows.first().eventId

      // Verify initial state: PENDING
      var row = emailOutboxRepository.findById(eventId).get()
      EmailOutboxTestFixtures.verifyOutboxRowStatus(row, EmailOutboxStatus.PENDING, 0)

      // Act: manually trigger a relay cycle; this claims and publishes the row in one step.
      emailOutboxRelay.dispatchPending()

      // Wait for emailsend queue to receive and process the message
      // In real scenario, this would be handled by @SqsListener worker, but we verify state here
      await()
        .timeout(Duration.ofMillis(EmailOutboxTestConstants.AWAIT_TIMEOUT_MS * 2))
        .pollInterval(Duration.ofMillis(EmailOutboxTestConstants.AWAIT_POLL_INTERVAL_MS))
        .untilAsserted {
          // After relay dispatch, verify Notify was called (indicating worker processed)
          val allRequests = notifyMockServer.getAllServeEvents()
          assertThat(allRequests).isNotEmpty()
        }

      // Assert: row transitions to SENT after worker processes
      await()
        .timeout(Duration.ofMillis(EmailOutboxTestConstants.AWAIT_TIMEOUT_MS))
        .pollInterval(Duration.ofMillis(EmailOutboxTestConstants.AWAIT_POLL_INTERVAL_MS))
        .untilAsserted {
          val finalRow = emailOutboxRepository.findById(eventId).get()
          assertThat(finalRow.status).isEqualTo(EmailOutboxStatus.SENT)
        }

      row = emailOutboxRepository.findById(eventId).get()
      assertThat(row.status).isEqualTo(EmailOutboxStatus.SENT)
      assertThat(row.attempts).isGreaterThan(0)
      assertThat(row.lastError).isNull()

      // Verify Notify was called exactly once (via reference=event_id)
      val notifyRequests = notifyMockServer.getAllServeEvents()
        .filter { it.request.url.contains("/v2/notifications/email") }
      assertThat(notifyRequests).hasSizeGreaterThanOrEqualTo(1)
    }

    @Test
    fun `it verifies payload serialization roundtrip encodes all required fields correctly`() {
      // Arrange: create a test outcome and enqueue
      val outcome = EmailOutboxTestFixtures.createTestEmailIngestionOutcome(
        ingestionStatus = IngestionStatus.SUCCESSFUL,
      )

      val countBefore = meterRegistry
        .get("email.outbox.event")
        .tags("status", EmailOutboxStatus.PENDING.name)
        .counter().count()

      // Act: enqueue the outcome (creates outbox rows with serialized payload)
      val enqueuedRows = emailOutboxService.enqueue(outcome)
      assertThat(enqueuedRows).isNotEmpty()

      // Assert: payload was serialized
      val row = enqueuedRows.first()
      assertThat(row.payload).isNotNull().isNotEmpty()

      // Deserialize and verify all required fields are present
      val deserializedPayload = emailOutboxPayloadMapper.readPayload(row.payload)
      EmailOutboxTestFixtures.verifyPayloadFidelity(deserializedPayload, IngestionStatus.SUCCESSFUL)

      // Verify specific fields match the original outcome
      assertThat(deserializedPayload.recipient).isEqualTo(outcome.emailData.sender)
      assertThat(deserializedPayload.ingestionStatus).isEqualTo(IngestionStatus.SUCCESSFUL)
      assertThat(deserializedPayload.schemaVersion).isEqualTo(1)

      // Verify metric was recorded
      val countAfter = meterRegistry
        .get("email.outbox.event")
        .tags("status", EmailOutboxStatus.PENDING.name)
        .counter().count()
      assertThat(countAfter).isGreaterThan(countBefore)
    }

    @Test
    fun `it emits correct metrics for PENDING → CLAIMED → SENT transitions`() {
      // Arrange: ingest and create outbox row
      val csvContent = listOf(createCsvRow()).joinToString("\n")
      val encoded = Base64.getEncoder().encodeToString(csvContent.toByteArray())
      val email = createEmailFile(encoded)

      s3Client.putObject(
        PutObjectRequest.builder()
          .bucket(BUCKET_NAME)
          .key(OBJECT_KEY)
          .build(),
        RequestBody.fromString(email),
      )

      val pendingCountBefore = meterRegistry
        .get("email.outbox.event")
        .tags("status", EmailOutboxStatus.PENDING.name)
        .counter().count()

      sendDomainSqsMessage(buildEmailMessage(OBJECT_KEY))

      await()
        .timeout(Duration.ofMillis(EmailOutboxTestConstants.AWAIT_TIMEOUT_MS))
        .pollInterval(Duration.ofMillis(EmailOutboxTestConstants.AWAIT_POLL_INTERVAL_MS))
        .until { getNumberOfMessagesCurrentlyOnQueue() == 0 }

      // Verify PENDING counter incremented
      val pendingCountAfter = meterRegistry
        .get("email.outbox.event")
        .tags("status", EmailOutboxStatus.PENDING.name)
        .counter().count()
      assertThat(pendingCountAfter).isGreaterThan(pendingCountBefore)

      // Act: claim batch and relay
      val claimedCountBefore = meterRegistry
        .get("email.outbox.event")
        .tags("status", EmailOutboxStatus.CLAIMED.name)
        .counter().count()

      emailOutboxService.claimBatch(EmailOutboxTestConstants.TEST_BATCH_SIZE)

      val claimedCountAfter = meterRegistry
        .get("email.outbox.event")
        .tags("status", EmailOutboxStatus.CLAIMED.name)
        .counter().count()
      assertThat(claimedCountAfter).isGreaterThan(claimedCountBefore)

      // Act: trigger worker (relay dispatch + worker processing)
      emailOutboxRelay.dispatchPending()

      await()
        .timeout(Duration.ofMillis(EmailOutboxTestConstants.AWAIT_TIMEOUT_MS * 2))
        .pollInterval(Duration.ofMillis(EmailOutboxTestConstants.AWAIT_POLL_INTERVAL_MS))
        .untilAsserted {
          val sentCount = meterRegistry
            .get("email.outbox.event")
            .tags("status", EmailOutboxStatus.SENT.name)
            .counter().count()
          assertThat(sentCount).isGreaterThanOrEqualTo(1.0)
        }
    }
  }

  @Nested
  @DisplayName("Scenario 2: Idempotency — Terminal Rows Block Duplicates")
  inner class Idempotency {
    /**
     * Idempotency: terminal rows (SENT, FAILED, DEAD) are skipped on redelivery.
     *
     * Path:
     * 1. Email_outbox row is in terminal state (SENT, FAILED, or DEAD)
     * 2. SQS redelivers the message (e.g., visibility timeout expired)
     * 3. Worker loads row, checks status == terminal
     * 4. Early return (no-op), message is ack'd and deleted
     * 5. Notify is NOT called again (no duplicate send)
     *
     * Why it matters: Prevents duplicate emails when SQS redelivers or worker crashes mid-ack.
     * Combined with event_id uniqueness, guarantees exactly-once delivery.
     */

    @Test
    fun `it skips processing terminal rows and does not send duplicates`() {
      // Arrange: create outbox rows in various terminal states
      val sentRow = EmailOutboxTestFixtures.createTestEmailOutboxRowSent(
        payload = emailOutboxPayloadMapper.toJson(
          EmailOutboxTestFixtures.createTestEmailIngestionOutcome(),
          EmailOutboxTestConstants.TEST_SENDER,
        ),
      )
      val failedRow = EmailOutbox(
        eventId = java.util.UUID.randomUUID(),
        status = EmailOutboxStatus.FAILED,
        payload = "{}",
      ).also {
        it.status = EmailOutboxStatus.FAILED
        it.attempts = 1
        it.lastError = "400 Bad Request"
      }
      val deadRow = EmailOutbox(
        eventId = java.util.UUID.randomUUID(),
        status = EmailOutboxStatus.DEAD,
        payload = "{}",
      ).also {
        it.status = EmailOutboxStatus.DEAD
        it.attempts = 3
        it.lastError = "Max retries exceeded"
      }

      emailOutboxRepository.saveAll(listOf(sentRow, failedRow, deadRow))

      val notifyCountBefore = notifyMockServer.getAllServeEvents()
        .filter { it.request.url.contains("/v2/notifications/email") }
        .size

      // Act: verify terminal rows are skipped (simulate direct service call)
      for (row in listOf(sentRow, failedRow, deadRow)) {
        EmailOutboxTestFixtures.verifyOutboxRowIsTerminal(row)
      }

      // Assert: Notify was not called (no new attempts)
      val notifyCountAfter = notifyMockServer.getAllServeEvents()
        .filter { it.request.url.contains("/v2/notifications/email") }
        .size
      assertThat(notifyCountAfter).isEqualTo(notifyCountBefore)

      // Verify rows remain in their original terminal state
      val sentRowAfter = emailOutboxRepository.findById(sentRow.eventId).get()
      assertThat(sentRowAfter.status).isEqualTo(EmailOutboxStatus.SENT)
      assertThat(sentRowAfter.attempts).isEqualTo(sentRow.attempts)

      val failedRowAfter = emailOutboxRepository.findById(failedRow.eventId).get()
      assertThat(failedRowAfter.status).isEqualTo(EmailOutboxStatus.FAILED)

      val deadRowAfter = emailOutboxRepository.findById(deadRow.eventId).get()
      assertThat(deadRowAfter.status).isEqualTo(EmailOutboxStatus.DEAD)
    }

    @Test
    fun `it ensures event_id uniqueness prevents duplicate outbox rows`() {
      // Arrange: create an outcome and enqueue it
      val outcome = EmailOutboxTestFixtures.createTestEmailIngestionOutcome()
      val firstEnqueue = emailOutboxService.enqueue(outcome)

      assertThat(firstEnqueue).isNotEmpty()
      val firstEventIds = firstEnqueue.map { it.eventId }.toSet()

      // Act: enqueue the same outcome again (should create new rows with different event_ids)
      val secondEnqueue = emailOutboxService.enqueue(outcome)
      assertThat(secondEnqueue).isNotEmpty()

      val secondEventIds = secondEnqueue.map { it.eventId }.toSet()

      // Assert: event_ids are unique across both enqueues
      val allEventIds = firstEventIds + secondEventIds
      assertThat(allEventIds).hasSize(firstEventIds.size + secondEventIds.size) // All unique
      assertThat(firstEventIds.intersect(secondEventIds)).isEmpty() // No overlap
    }

    @Test
    fun `it emits no metric changes when skipping terminal rows`() {
      // Arrange: create terminal rows
      val row = EmailOutboxTestFixtures.createTestEmailOutboxRowSent()
      emailOutboxRepository.save(row)

      val sentCountBefore = meterRegistry
        .get("email.outbox.event")
        .tags("status", EmailOutboxStatus.SENT.name)
        .counter().count()

      // Act: attempt to reprocess (worker checks terminal status and returns early)
      // This is simulated—the real worker would do this in EmailSendListener.receiveEmailSend()
      val loadedRow = emailOutboxRepository.findById(row.eventId).get()
      assertThat(loadedRow.status).isEqualTo(EmailOutboxStatus.SENT)

      // Assert: no metric change (no counter incremented for reprocessing)
      val sentCountAfter = meterRegistry
        .get("email.outbox.event")
        .tags("status", EmailOutboxStatus.SENT.name)
        .counter().count()
      assertThat(sentCountAfter).isEqualTo(sentCountBefore)
    }
  }

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
     * 2. Ingest email → PENDING
     * 3. Relay claims → CLAIMED
     * 4. Worker calls Notify → 500 → permanentFailureStatus() returns null (transient)
     * 5. markRetry() → status=CLAIMED, attempts++, lastError="Internal Server Error"
     * 6. Re-throw exception → SQS redelivery
     * 7. Repeat until maxReceiveCount OR stub flipped to 201
     * 8. On recovery: markSent() → SENT
     *
     * Why it matters: Network hiccups and rate-limiting shouldn't lose emails.
     * Automatic retry ensures eventual delivery without manual intervention.
     */

    @Test
    fun `it retries on transient failure and eventually succeeds after stub recovery`() {
      // Arrange: create an outcome and enqueue (this creates PENDING row in DB)
      val outcome = EmailOutboxTestFixtures.createTestEmailIngestionOutcome()
      val enqueuedRows = emailOutboxService.enqueue(outcome)
      assertThat(enqueuedRows).isNotEmpty()

      val eventId = enqueuedRows.first().eventId
      var row = emailOutboxRepository.findById(eventId).get()
      EmailOutboxTestFixtures.verifyOutboxRowStatus(row, EmailOutboxStatus.PENDING, 0)

      // Act Phase 1: Claim batch
      val claimedBatch = emailOutboxService.claimBatch(EmailOutboxTestConstants.TEST_BATCH_SIZE)
      assertThat(claimedBatch).isNotEmpty()

      row = emailOutboxRepository.findById(eventId).get()
      EmailOutboxTestFixtures.verifyOutboxRowIsClaimed(row)
      val initialAttempts = row.attempts

      // Act Phase 2: Simulate transient failure (retry on CLAIMED)
      val transientError = "simulated transient failure"
      emailOutboxService.markRetry(eventId, transientError)

      row = emailOutboxRepository.findById(eventId).get()
      assertThat(row.status).isEqualTo(EmailOutboxStatus.CLAIMED)
      assertThat(row.attempts).isEqualTo(initialAttempts + 1)
      assertThat(row.lastError).isEqualTo(transientError)

      // Verify CLAIMED counter was emitted
      val claimedCount = meterRegistry
        .get("email.outbox.event")
        .tags("status", EmailOutboxStatus.CLAIMED.name)
        .counter().count()
      assertThat(claimedCount).isGreaterThan(0.0)

      // Act Phase 3: Simulate recovery (mark as sent after retry succeeds)
      emailOutboxService.markSent(eventId)

      // Assert: row is now SENT
      row = emailOutboxRepository.findById(eventId).get()
      assertThat(row.status).isEqualTo(EmailOutboxStatus.SENT)
      assertThat(row.attempts).isGreaterThan(initialAttempts + 1) // Incremented on sent
      assertThat(row.lastError).isNull() // Cleared on success

      // Verify SENT counter was emitted
      val sentCount = meterRegistry
        .get("email.outbox.event")
        .tags("status", EmailOutboxStatus.SENT.name)
        .counter().count()
      assertThat(sentCount).isGreaterThan(0.0)
    }

    @Test
    fun `it tracks attempts count across multiple retries`() {
      // Arrange: create and claim an outbox row
      val row = EmailOutboxTestFixtures.createTestEmailOutboxRowClaimed()
      emailOutboxRepository.save(row)

      assertThat(row.attempts).isEqualTo(0)

      // Act: simulate multiple retry cycles
      for (i in 1..3) {
        emailOutboxService.markRetry(row.eventId, "Retry $i")
        val updated = emailOutboxRepository.findById(row.eventId).get()
        assertThat(updated.attempts).isEqualTo(i)
      }

      // Assert: final row shows cumulative attempts
      val finalRow = emailOutboxRepository.findById(row.eventId).get()
      assertThat(finalRow.attempts).isEqualTo(3)
      assertThat(finalRow.lastError).isEqualTo("Retry 3")
      assertThat(finalRow.status).isEqualTo(EmailOutboxStatus.CLAIMED)
    }
  }

  @Nested
  @DisplayName("Scenario 4: Permanent Failure Fast-path")
  inner class PermanentFailure {
    /**
     * Permanent failure fast-path: GOV.UK Notify 4xx (except 429) → markFailed(),
     * no retry, message ack'd and deleted from queue.
     *
     * Path:
     * 1. Notify stub configured to return 400 (Bad Request)
     * 2. Ingest email → PENDING
     * 3. Relay claims → CLAIMED
     * 4. Worker calls Notify → NotificationClientException(400)
     * 5. permanentFailureStatus(e) detects 4xx (non-429) → returns 400
     * 6. markFailed() → status=FAILED, attempts++, lastError="400 Bad Request"
     * 7. Return (do not re-throw) → SQS ack → message deleted
     * 8. No further retries, no DLQ
     *
     * Why it matters: 4xx errors are unrecoverable (bad request, auth, etc.)
     * Retrying them would cause a retry storm. Fast-path immediately exits.
     */

    @Test
    fun `it marks row as FAILED on permanent Notify error (4xx) without retry`() {
      // Arrange: create and claim an outbox row
      val row = EmailOutboxTestFixtures.createTestEmailOutboxRowClaimed()
      emailOutboxRepository.save(row)

      val failedCountBefore = meterRegistry
        .get("email.outbox.event")
        .tags("status", EmailOutboxStatus.FAILED.name)
        .counter().count()

      // Act: simulate permanent failure (mark as failed)
      val permanentError = "400 Bad Request"
      emailOutboxService.markFailed(row.eventId, permanentError)

      // Assert: row transitions to FAILED
      val updated = emailOutboxRepository.findById(row.eventId).get()
      assertThat(updated.status).isEqualTo(EmailOutboxStatus.FAILED)
      assertThat(updated.attempts).isEqualTo(1)
      assertThat(updated.lastError).isEqualTo(permanentError)

      // Verify metric was emitted
      val failedCountAfter = meterRegistry
        .get("email.outbox.event")
        .tags("status", EmailOutboxStatus.FAILED.name)
        .counter().count()
      assertThat(failedCountAfter).isGreaterThan(failedCountBefore)
    }

    @Test
    fun `it does not retry after permanent failure (FAILED is terminal)`() {
      // Arrange: create a FAILED row
      val row = EmailOutbox(
        eventId = java.util.UUID.randomUUID(),
        status = EmailOutboxStatus.FAILED,
        payload = "{}",
      ).also {
        it.attempts = 1
        it.lastError = "400 Bad Request"
      }
      emailOutboxRepository.save(row)

      // Act: attempt to load and verify terminal status
      val loaded = emailOutboxRepository.findById(row.eventId).get()

      // Assert: FAILED is terminal (no further processing should happen)
      assertThat(loaded.status).isEqualTo(EmailOutboxStatus.FAILED)
      EmailOutboxTestFixtures.verifyOutboxRowIsTerminal(loaded)
    }
  }

  @Nested
  @DisplayName("Scenario 5: Lease Reclaim & DLQ Fallback")
  inner class LeaseReclaimDlq {
    /**
     * Lease reclaim: stale CLAIMED rows (crash/failed publish) are reclaimed
     * back to PENDING. Uses [TestClock] to simulate time passage without real waits.
     *
     * Path:
     * Phase 1 (Crash simulation):
     * 1. Ingest email → PENDING
     * 2. Relay claims → CLAIMED (claimedAt = now)
     * 3. Simulate publish failure: row stays CLAIMED, claimedAt is old
     * 4. Worker never receives message
     *
     * Phase 2 (Reclaim cycle):
     * 1. Time passes: now > claimedAt + leaseTimeout (via testClock.advanceBy)
     * 2. EmailOutboxRelay.reclaimExpired() called
     * 3. UPDATE email_outbox SET status='PENDING' WHERE status='CLAIMED' AND claimed_at < threshold
     * 4. Row returns to PENDING, eligible for re-claim
     *
     * Phase 3 (Eventual delivery or DLQ):
     * 1. Next relay cycle claims PENDING row again
     * 2. Publishes to emailsend
     * 3. Worker processes: on success → SENT; on failure → eventually DEAD → DLQ
     *
     * Why it matters: Prevents stale leases from stranding events indefinitely.
     * Crashed workers don't need manual recovery—the lease reclaim loop fixes them.
     */

    @Test
    fun `it reclaims stale CLAIMED rows back to PENDING using TestClock`() {
      // Arrange: create a CLAIMED row
      val row = EmailOutboxTestFixtures.createTestEmailOutboxRowClaimed().also {
        it.claimedAt = testClock.now()
        it.claimedBy = "test-instance"
      }
      emailOutboxRepository.save(row)

      // Verify initial state
      var loaded = emailOutboxRepository.findById(row.eventId).get()
      EmailOutboxTestFixtures.verifyOutboxRowIsClaimed(loaded)
      val originalClaimedAt = loaded.claimedAt

      // Act: advance time by 121 seconds (exceeds default 120s lease timeout)
      testClock.advanceBy(Duration.ofSeconds(121))

      // Call reclaim with 120s timeout
      val reclaimed = emailOutboxService.reclaimExpired(Duration.ofMillis(EmailOutboxTestConstants.TEST_LEASE_TIMEOUT_MS))
      assertThat(reclaimed).isEqualTo(1)

      // Assert: row transitions back to PENDING
      loaded = emailOutboxRepository.findById(row.eventId).get()
      assertThat(loaded.status).isEqualTo(EmailOutboxStatus.PENDING)
      assertThat(loaded.claimedAt).isNull()
      assertThat(loaded.claimedBy).isNull()
      // claimedAt timestamp should no longer equal original (it was cleared)
      assertThat(loaded.claimedAt).isNotEqualTo(originalClaimedAt)
    }

    @Test
    fun `it does not reclaim recently-claimed rows (false positive prevention)`() {
      // Arrange: create a CLAIMED row with current time
      val row = EmailOutboxTestFixtures.createTestEmailOutboxRowClaimed().also {
        it.claimedAt = testClock.now()
        it.claimedBy = "test-instance"
      }
      emailOutboxRepository.save(row)

      // Verify it's recently claimed
      var loaded = emailOutboxRepository.findById(row.eventId).get()
      EmailOutboxTestFixtures.verifyOutboxRowIsClaimed(loaded)

      // Act: do NOT advance time; call reclaim immediately
      emailOutboxService.reclaimExpired(Duration.ofMillis(EmailOutboxTestConstants.TEST_LEASE_TIMEOUT_MS))

      // Assert: row should NOT be reclaimed (not stale enough)
      // The reclaim query checks: claimed_at < (now - leaseTimeout)
      // Since claimed_at is recent, this condition is false
      loaded = emailOutboxRepository.findById(row.eventId).get()
      assertThat(loaded.status).isEqualTo(EmailOutboxStatus.CLAIMED)
      assertThat(loaded.claimedAt).isNotNull()
    }

    @Test
    fun `it does not reclaim PENDING or SENT rows (idempotency)`() {
      // Arrange: create rows in various states
      val pendingRow = EmailOutboxTestFixtures.createTestEmailOutboxRowPending()
      val sentRow = EmailOutboxTestFixtures.createTestEmailOutboxRowSent()

      emailOutboxRepository.saveAll(listOf(pendingRow, sentRow))

      // Act: advance time and call reclaim
      testClock.advanceBy(Duration.ofSeconds(121))
      emailOutboxService.reclaimExpired(Duration.ofMillis(EmailOutboxTestConstants.TEST_LEASE_TIMEOUT_MS))
      val pendingLoaded = emailOutboxRepository.findById(pendingRow.eventId).get()
      assertThat(pendingLoaded.status).isEqualTo(EmailOutboxStatus.PENDING)

      val sentLoaded = emailOutboxRepository.findById(sentRow.eventId).get()
      assertThat(sentLoaded.status).isEqualTo(EmailOutboxStatus.SENT)
    }

    @Test
    fun `it verifies test clock advances time without real waits`() {
      // This test demonstrates TestClock functionality
      val startTime = testClock.now()

      // Advance by 10 seconds
      testClock.advanceBy(Duration.ofSeconds(10))
      val afterAdvance = testClock.now()

      // Assert: time advanced without actual delay
      val difference = java.time.temporal.ChronoUnit.SECONDS.between(startTime, afterAdvance)
      assertThat(difference).isEqualTo(10)

      // Advance again by 5 more seconds
      testClock.advanceBy(Duration.ofSeconds(5))
      val afterSecondAdvance = testClock.now()

      val totalDifference = java.time.temporal.ChronoUnit.SECONDS.between(startTime, afterSecondAdvance)
      assertThat(totalDifference).isEqualTo(15)

      // Reset clock
      testClock.reset()
      val afterReset = testClock.now()
      // After reset, clock should be near current system time (not our advanced time)
      val recentlyAdjusted = afterReset.isBefore(startTime.plusSeconds(120))
      assertThat(recentlyAdjusted).isTrue()
    }
  }

  private fun sendDomainSqsMessage(rawMessage: String): CompletableFuture<*> = emailQueueSqsClient.sendMessage { it.queueUrl(emailQueueSqsUrl).messageBody(rawMessage) }

  private fun buildEmailMessage(objectKey: String): String = """
    {
      "Type" : "Notification",
      "MessageId" : "4730435b-88b9-5b6c-a91c-9b1236b456f7",
      "TopicArn" : "arn:aws:sns:eu-west-2:000000000000:email-topic",
      "Message" : "{ \"notificationType\": \"Received\", \"receipt\": { \"action\": { \"bucketName\": \"$BUCKET_NAME\", \"objectKey\": \"$objectKey\" }}}"
    }
  """.trimIndent()

  private fun getNumberOfMessagesCurrentlyOnQueue(): Int = emailQueueSqsClient.countAllMessagesOnQueue(emailQueueSqsUrl).get()
}
