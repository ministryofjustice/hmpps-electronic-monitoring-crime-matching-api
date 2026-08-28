package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.outbox

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.EmailSendMessage
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import java.util.UUID

/** Publishes outbox events onto the shared `email` queue for the idempotent send worker. */
@Service
class EmailSendQueueService(
  private val hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
) {
  companion object {
    const val QUEUE_ID = "email"
  }

  private val emailSendQueue by lazy {
    hmppsQueueService.findByQueueId(QUEUE_ID) ?: throw IllegalStateException("$QUEUE_ID queue not found")
  }

  fun publish(eventId: UUID) {
    emailSendQueue.sqsClient.sendMessage(
      SendMessageRequest.builder()
        .queueUrl(emailSendQueue.queueUrl)
        .messageBody(objectMapper.writeValueAsString(EmailSendMessage(eventId)))
        .build(),
    ).get()
  }
}
