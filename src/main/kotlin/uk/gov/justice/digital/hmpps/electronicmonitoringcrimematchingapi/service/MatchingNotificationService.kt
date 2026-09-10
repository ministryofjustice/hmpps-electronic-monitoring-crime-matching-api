package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.exception.PublishEventException
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.MatchingNotification
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.PublishMatchingOutbox
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PublishMatchingState
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.publishMatching.PublishMatchingOutboxRepository
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.publish
import java.time.Instant

@Service
class MatchingNotificationService(
  private val hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
  private val publishMatchingOutboxRepository: PublishMatchingOutboxRepository,
) {
  companion object {
    const val TOPIC_ID = "matchingnotificationstopic"
    const val CRIME_MATCHING_REQUEST = "CRIME_MATCHING_REQUEST"
  }

  private val log = LoggerFactory.getLogger(this::class.java)

  private val matchingNotificationsTopic by lazy {
    hmppsQueueService.findByTopicId(TOPIC_ID) ?: throw IllegalStateException("$TOPIC_ID not found")
  }

  private fun publish(payloadEvent: MatchingNotification) = try {
    matchingNotificationsTopic.publish(
      payloadEvent.type,
      objectMapper.writeValueAsString(payloadEvent),
    )
  } catch (e: Throwable) {
    val message = "Failed to publish Event $payloadEvent.eventType to $TOPIC_ID"
    log.error(message, e)
    throw PublishEventException(message, e)
  }

  // Using the outbox table for Publish Matching, submit all eligible Publish Matching requests
  // We want to guarantee at-least-once delivery. Some requests may be published more than once.
  fun publishMatchingRequests() {
    val claimedRows = claimEligibleOutboxRows()
    claimedRows.forEach { row ->
      val payloadEvent = objectMapper.readValue(row.payload, MatchingNotification::class.java)
      try {
        publish(payloadEvent)
        completeClaimedRow(row, PublishMatchingState.PUBLISHED, null)
      } catch (e: Throwable) {
        completeClaimedRow(row, PublishMatchingState.FAILED, e.message)
      }
    }
  }

  private fun completeClaimedRow(
    row: PublishMatchingOutbox,
    state: PublishMatchingState,
    lastError: String?,
  ) {
    val claimedAt = row.claimedAt?.toEpochMilli()
    if (claimedAt == null) {
      log.warn("Skipping PublishMatchingOutbox completion for row {} because claimedAt is null", row.id)
      return
    }

    val updateCount = publishMatchingOutboxRepository.completeClaimedRow(
      id = row.id,
      claimedAt = claimedAt,
      state = state.name,
      attempts = row.attempts + 1,
      lastError = lastError,
      version = row.version,
    )

    if (updateCount == 0) {
      log.debug("PublishMatchingOutbox row {} completion skipped: claim/version no longer owned", row.id)
    }
  }

  @Transactional
  fun claimEligibleOutboxRows(): List<PublishMatchingOutbox> {
    val now = Instant.now()
    val cutoff = now.minusSeconds(60)

    return publishMatchingOutboxRepository.claimEligibleRows(
      pendingState = PublishMatchingState.PENDING.name,
      cutoff = cutoff.toEpochMilli(),
      now = now.toEpochMilli(),
    )
  }

  @Transactional
  fun createMatchingRequest(crimeBatchId: String) {
    val payloadEvent = objectMapper.writeValueAsString(
      MatchingNotification(
        type = CRIME_MATCHING_REQUEST,
        crimeBatchId = crimeBatchId,
      ),
    )
    publishMatchingOutboxRepository.save(
      PublishMatchingOutbox(
        payload = payloadEvent,
        state = PublishMatchingState.PENDING,
      ),
    )
  }
}
