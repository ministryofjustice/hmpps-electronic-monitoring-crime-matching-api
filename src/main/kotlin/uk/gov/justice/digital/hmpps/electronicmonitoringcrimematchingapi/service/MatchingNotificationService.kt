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

  fun publishMatchingRequest(crimeBatchId: String) {
    val payloadEvent = MatchingNotification(
      type = CRIME_MATCHING_REQUEST,
      crimeBatchId = crimeBatchId,
    )
    publish(payloadEvent)
    savePublishMatchingOutboxState(payloadEvent, PublishMatchingState.PUBLISHED)
  }

  @Transactional
  fun savePublishMatchingOutboxState(payloadEvent: MatchingNotification, state: PublishMatchingState) {
    val serialisedPayload = objectMapper.writeValueAsString(payloadEvent)
    val outboxRows: List<PublishMatchingOutbox> = publishMatchingOutboxRepository.findAllByPayload(serialisedPayload)
    outboxRows.forEach {
      it.state = state
      publishMatchingOutboxRepository.save(
        it,
      )
    }
  }
}
