package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal

import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.EmailQueueMessageType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.EmailSendMessage
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.SqsMessage

/**
 * Routes messages from the shared email queue to the correct domain handler.
 *
 * Message type is determined by the explicit [EmailQueueMessageType] discriminator field.
 * SNS-wrapped ingestion messages (from the email topic subscription) have no [messageType]
 * field and are always routed to [EmailListener].
 */
@Service
class EmailQueueRoutingListener(
  private val objectMapper: ObjectMapper,
  private val emailListener: EmailListener,
  private val emailSendListener: EmailSendListener,
) {
  @SqsListener("email", factory = "hmppsQueueContainerFactoryProxy")
  fun route(
    rawMessage: String,
    @Header(value = "ApproximateReceiveCount", required = false) receiveCount: String?,
  ) {
    val payload = objectMapper.readTree(rawMessage)
    val messageType = payload.get("messageType")?.asText()
      ?.let { runCatching { EmailQueueMessageType.valueOf(it) }.getOrNull() }

    when (messageType) {
      EmailQueueMessageType.EMAILSEND -> {
        emailSendListener.receiveEmailSend(
          objectMapper.treeToValue(payload, EmailSendMessage::class.java),
          receiveCount,
        )
      }
      null -> {
        // No messageType — treat as an SNS-wrapped ingestion notification.
        if (!payload.has("Message")) {
          throw IllegalArgumentException("Unsupported message schema for shared email queue: no messageType and no SNS Message field")
        }
        emailListener.receiveEmailNotification(objectMapper.treeToValue(payload, SqsMessage::class.java))
      }
    }
  }
}
