package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.exception.PublishEventException
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.MatchingNotification
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchIngestionAttempt
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.PublishMatchingOutbox
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.IngestionStatus
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

  fun publishMatchingRequestIfRequired(crimeBatchId: String, ingestionStatus: IngestionStatus, ingestionAttempt: CrimeBatchIngestionAttempt) {
    if (ingestionStatus == IngestionStatus.SUCCESSFUL || ingestionStatus == IngestionStatus.PARTIAL) {
      val outboxRow = savePendingState(ingestionAttempt)
      publish(
        MatchingNotification(
          type = CRIME_MATCHING_REQUEST,
          crimeBatchId = crimeBatchId,
        ),
      )
      savePublishedState(outboxRow)
    } else {
      saveNotRequiredState(ingestionAttempt)
    }
  }

  @Transactional
  private fun saveNotRequiredState(ingestionAttempt: CrimeBatchIngestionAttempt) {
    publishMatchingOutboxRepository.save(
      PublishMatchingOutbox(
        crimeBatchIngestionAttempt = ingestionAttempt,
        state = PublishMatchingState.NOT_REQUIRED,
      ),
    )
  }

  @Transactional
  private fun savePendingState(ingestionAttempt: CrimeBatchIngestionAttempt): PublishMatchingOutbox = publishMatchingOutboxRepository.save(
    PublishMatchingOutbox(
      crimeBatchIngestionAttempt = ingestionAttempt,
      state = PublishMatchingState.PENDING_OR_UNCONFIRMED,
    ),
  )

  @Transactional
  private fun savePublishedState(outboxRow: PublishMatchingOutbox) {
    outboxRow.state = PublishMatchingState.PUBLISHED
    publishMatchingOutboxRepository.save(
      outboxRow,
    )
  }
}
