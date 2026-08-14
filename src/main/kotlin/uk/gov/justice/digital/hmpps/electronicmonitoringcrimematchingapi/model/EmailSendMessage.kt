package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model

import java.util.UUID

/** Body of the message placed on the `emailsend` queue by the outbox relay. */
data class EmailSendMessage(
  val eventId: UUID,
)
