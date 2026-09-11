package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.repository.publishMatching

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.PublishMatchingOutbox
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PublishMatchingState
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.publishMatching.PublishMatchingOutboxRepository
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@ActiveProfiles("integration")
class PublishMatchingOutboxRepositoryTest : IntegrationTestBase() {

  @Autowired
  lateinit var publishMatchingOutboxRepository: PublishMatchingOutboxRepository

  @Autowired
  lateinit var transactionTemplate: TransactionTemplate

  @BeforeEach
  fun setup() {
    publishMatchingOutboxRepository.deleteAll()
  }

  @Test
  fun `it should claim only pending rows that are unclaimed or claimed before cutoff`() {
    val now = Instant.parse("2026-01-01T00:10:00Z")
    val cutoff = now.minusSeconds(60)

    val eligibleUnclaimed = givenOutboxRow(
      state = PublishMatchingState.PENDING,
      claimedAt = null,
      createdAt = Instant.parse("2026-01-01T00:00:00Z"),
    )
    val eligibleStaleClaim = givenOutboxRow(
      state = PublishMatchingState.PENDING,
      claimedAt = cutoff.minusMillis(1),
      createdAt = Instant.parse("2026-01-01T00:01:00Z"),
    )
    val ineligibleRecentClaim = givenOutboxRow(
      state = PublishMatchingState.PENDING,
      claimedAt = cutoff.plusMillis(1),
      createdAt = Instant.parse("2026-01-01T00:02:00Z"),
    )
    val ineligiblePublished = givenOutboxRow(
      state = PublishMatchingState.PUBLISHED,
      claimedAt = null,
      createdAt = Instant.parse("2026-01-01T00:03:00Z"),
    )

    val claimedRows = publishMatchingOutboxRepository.claimEligibleRows(
      pendingState = PublishMatchingState.PENDING.name,
      cutoff = cutoff.toEpochMilli(),
      now = now.toEpochMilli(),
    )

    assertThat(claimedRows.map { it.id }).containsExactlyInAnyOrder(
      eligibleUnclaimed.id,
      eligibleStaleClaim.id,
    )
    assertThat(claimedRows).allSatisfy { claimed ->
      assertThat(claimed.claimedAt).isEqualTo(now)
      assertThat(claimed.state).isEqualTo(PublishMatchingState.PENDING)
    }

    val persistedRows = publishMatchingOutboxRepository.findAllById(
      listOf(
        eligibleUnclaimed.id,
        eligibleStaleClaim.id,
        ineligibleRecentClaim.id,
        ineligiblePublished.id,
      ),
    ).associateBy { it.id }

    assertThat(persistedRows[eligibleUnclaimed.id]!!.claimedAt).isEqualTo(now)
    assertThat(persistedRows[eligibleStaleClaim.id]!!.claimedAt).isEqualTo(now)
    assertThat(persistedRows[ineligibleRecentClaim.id]!!.claimedAt).isEqualTo(cutoff.plusMillis(1))
    assertThat(persistedRows[ineligiblePublished.id]!!.claimedAt).isNull()
  }

  @Test
  fun `it should return no rows when there are no eligible rows`() {
    val now = Instant.parse("2026-01-01T00:10:00Z")
    val cutoff = now.minusSeconds(60)

    val pendingRecentClaim = givenOutboxRow(
      state = PublishMatchingState.PENDING,
      claimedAt = cutoff.plusMillis(5),
    )
    val published = givenOutboxRow(
      state = PublishMatchingState.PUBLISHED,
      claimedAt = null,
    )

    val claimedRows = publishMatchingOutboxRepository.claimEligibleRows(
      pendingState = PublishMatchingState.PENDING.name,
      cutoff = cutoff.toEpochMilli(),
      now = now.toEpochMilli(),
    )

    assertThat(claimedRows).isEmpty()

    val persistedRows = publishMatchingOutboxRepository.findAllById(
      listOf(pendingRecentClaim.id, published.id),
    ).associateBy { it.id }

    assertThat(persistedRows[pendingRecentClaim.id]!!.claimedAt).isEqualTo(cutoff.plusMillis(5))
    assertThat(persistedRows[published.id]!!.claimedAt).isNull()
  }

  @Test
  fun `it should claim at most two eligible rows`() {
    val now = Instant.parse("2026-01-01T00:10:00Z")
    val cutoff = now.minusSeconds(60)

    val eligibleOne = givenOutboxRow(
      state = PublishMatchingState.PENDING,
      claimedAt = null,
      createdAt = Instant.parse("2026-01-01T00:00:00Z"),
    )
    val eligibleTwo = givenOutboxRow(
      state = PublishMatchingState.PENDING,
      claimedAt = null,
      createdAt = Instant.parse("2026-01-01T00:01:00Z"),
    )
    val eligibleThree = givenOutboxRow(
      state = PublishMatchingState.PENDING,
      claimedAt = null,
      createdAt = Instant.parse("2026-01-01T00:02:00Z"),
    )

    val claimedRows = publishMatchingOutboxRepository.claimEligibleRows(
      pendingState = PublishMatchingState.PENDING.name,
      cutoff = cutoff.toEpochMilli(),
      now = now.toEpochMilli(),
    )

    assertThat(claimedRows.map { it.id }).containsExactlyInAnyOrder(eligibleOne.id, eligibleTwo.id)

    val persistedRows = publishMatchingOutboxRepository.findAllById(
      listOf(eligibleOne.id, eligibleTwo.id, eligibleThree.id),
    ).associateBy { it.id }

    assertThat(persistedRows[eligibleOne.id]!!.claimedAt).isEqualTo(now)
    assertThat(persistedRows[eligibleTwo.id]!!.claimedAt).isEqualTo(now)
    assertThat(persistedRows[eligibleThree.id]!!.claimedAt).isNull()
  }

  @Test
  fun `it should skip a row already locked by another claim transaction`() {
    val row = givenOutboxRow(
      state = PublishMatchingState.PENDING,
      claimedAt = null,
    )
    val cutoff = Instant.parse("2026-01-01T00:09:00Z")
    val firstClaimNow = Instant.parse("2026-01-01T00:10:00Z")
    val secondClaimNow = Instant.parse("2026-01-01T00:10:30Z")

    val firstClaimedRows = AtomicReference<List<PublishMatchingOutbox>>(emptyList())
    val secondClaimedRows = AtomicReference<List<PublishMatchingOutbox>>(emptyList())
    val firstClaimComplete = CountDownLatch(1)
    val secondClaimAttempted = CountDownLatch(1)
    val executor = Executors.newFixedThreadPool(2)

    try {
      val firstFuture = executor.submit {
        transactionTemplate.executeWithoutResult {
          firstClaimedRows.set(
            publishMatchingOutboxRepository.claimEligibleRows(
              pendingState = PublishMatchingState.PENDING.name,
              cutoff = cutoff.toEpochMilli(),
              now = firstClaimNow.toEpochMilli(),
            ),
          )
          firstClaimComplete.countDown()
          assertThat(secondClaimAttempted.await(5, TimeUnit.SECONDS)).isTrue()
        }
      }

      val secondFuture = executor.submit {
        assertThat(firstClaimComplete.await(5, TimeUnit.SECONDS)).isTrue()
        transactionTemplate.executeWithoutResult {
          secondClaimedRows.set(
            publishMatchingOutboxRepository.claimEligibleRows(
              pendingState = PublishMatchingState.PENDING.name,
              cutoff = cutoff.toEpochMilli(),
              now = secondClaimNow.toEpochMilli(),
            ),
          )
        }
        secondClaimAttempted.countDown()
      }

      firstFuture.get(10, TimeUnit.SECONDS)
      secondFuture.get(10, TimeUnit.SECONDS)
    } finally {
      executor.shutdownNow()
    }

    assertThat(firstClaimedRows.get().map { it.id }).containsExactly(row.id)
    assertThat(secondClaimedRows.get()).isEmpty()

    val persistedRow = publishMatchingOutboxRepository.findById(row.id).orElseThrow()
    assertThat(persistedRow.claimedAt).isEqualTo(firstClaimNow)
  }

  @Test
  fun `it should complete a claimed row when claim timestamp and version both match`() {
    val claimedAt = Instant.parse("2026-01-01T00:10:00Z")
    val row = givenOutboxRow(
      state = PublishMatchingState.PENDING,
      claimedAt = claimedAt,
    )

    val updated = publishMatchingOutboxRepository.completeClaimedRow(
      id = row.id,
      claimedAt = claimedAt.toEpochMilli(),
      state = PublishMatchingState.PUBLISHED.name,
      attempts = 1,
      lastError = null,
      version = row.version,
    )

    assertThat(updated).isEqualTo(1)

    val persisted = publishMatchingOutboxRepository.findById(row.id).orElseThrow()
    assertThat(persisted.state).isEqualTo(PublishMatchingState.PUBLISHED)
    assertThat(persisted.attempts).isEqualTo(1)
    assertThat(persisted.lastError).isNull()
    assertThat(persisted.version).isEqualTo(1)
  }

  @Test
  fun `it should ignore completion when claim timestamp or version no longer match`() {
    val claimedAt = Instant.parse("2026-01-01T00:10:00Z")
    val row = givenOutboxRow(
      state = PublishMatchingState.PENDING,
      claimedAt = claimedAt,
    )

    val updatedWithWrongClaim = publishMatchingOutboxRepository.completeClaimedRow(
      id = row.id,
      claimedAt = claimedAt.plusSeconds(1).toEpochMilli(),
      state = PublishMatchingState.FAILED.name,
      attempts = 1,
      lastError = "stale claim",
      version = row.version,
    )
    val updatedWithWrongVersion = publishMatchingOutboxRepository.completeClaimedRow(
      id = row.id,
      claimedAt = claimedAt.toEpochMilli(),
      state = PublishMatchingState.FAILED.name,
      attempts = 1,
      lastError = "stale version",
      version = row.version + 1,
    )

    assertThat(updatedWithWrongClaim).isZero()
    assertThat(updatedWithWrongVersion).isZero()

    val persisted = publishMatchingOutboxRepository.findById(row.id).orElseThrow()
    assertThat(persisted.state).isEqualTo(PublishMatchingState.PENDING)
    assertThat(persisted.attempts).isZero()
    assertThat(persisted.lastError).isNull()
    assertThat(persisted.version).isEqualTo(0)
  }

  private fun givenOutboxRow(
    state: PublishMatchingState,
    claimedAt: Instant?,
    createdAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
  ): PublishMatchingOutbox = publishMatchingOutboxRepository.save(
    PublishMatchingOutbox(
      payload = "{\"type\":\"CRIME_MATCHING_REQUEST\",\"crime_batch_id\":\"batch-id\"}",
      state = state,
      claimedAt = claimedAt,
      createdAt = createdAt,
    ),
  )
}
