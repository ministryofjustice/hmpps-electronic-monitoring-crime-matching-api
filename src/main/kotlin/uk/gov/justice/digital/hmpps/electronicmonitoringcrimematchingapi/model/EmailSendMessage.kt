package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model

import java.util.UUID

enum class EmailQueueMessageType {
  EMAILSEND,
}

/**
 * Body of the message placed on the shared email queue by the outbox relay.
 * [messageType] is a discriminator used by [EmailQueueRoutingListener] to route messages
 * unambiguously without relying on structural field sniffing.
 */
data class EmailSendMessage(
  val eventId: UUID,
  val messageType: EmailQueueMessageType = EmailQueueMessageType.EMAILSEND,
)
