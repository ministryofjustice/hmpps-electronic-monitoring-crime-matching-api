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
import java.time.LocalDateTime

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

  fun publishMatchingRequests() {
    val claimedRows = claimEligibleOutboxRows()
    claimedRows.forEach { row ->
      val payloadEvent = objectMapper.readValue(row.payload, MatchingNotification::class.java)
      try {
        publish(payloadEvent)

        row.state = PublishMatchingState.PUBLISHED
        row.attempts += 1
        row.lastError = null
        publishMatchingOutboxRepository.save(row)
      } catch (e: Throwable) {
        row.state = PublishMatchingState.FAILED
        row.attempts += 1
        row.lastError = e.message
        publishMatchingOutboxRepository.save(row)
      }
    }
  }

  @Transactional
  fun claimEligibleOutboxRows(): List<PublishMatchingOutbox> {
    val now = LocalDateTime.now()
    val cutoff = now.minusMinutes(1)

    return publishMatchingOutboxRepository.claimEligibleRows(
      pendingState = PublishMatchingState.PENDING.name,
      cutoff = cutoff,
      now = now,
    )
  }

  @Transactional
  fun savePublishMatchingRequest(payloadEvent: MatchingNotification) {
    publishMatchingOutboxRepository.save(
      PublishMatchingOutbox(
        payload = objectMapper.writeValueAsString(payloadEvent),
        state = PublishMatchingState.PENDING,
      ),
    )
  }
}
