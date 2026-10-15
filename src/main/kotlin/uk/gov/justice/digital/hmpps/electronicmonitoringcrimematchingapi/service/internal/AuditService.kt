package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.HmppsAuditEvent
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService

// Guidance for what to send to HMPPS Audit: https://dsdmoj.atlassian.net/wiki/x/bIC2SgE
// Example: "Search interactions: when a user performs a search for information."
// Example: "Data interactions: when a user creates, views updates or deletes information."
// "In most cases, audit events should be generated from the user interface, as this is where the user action originates.
// However, if an action can occur outside the UI (for example through an API or background process), you should ensure those interactions are also audited appropriately."
@Service
@Component
class AuditService(
  private val hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
) {
  private val auditQueue by lazy { hmppsQueueService.findByQueueId("audit") as HmppsQueue }
  private val auditSqsClient by lazy { auditQueue.sqsClient }
  private val auditQueueUrl by lazy { auditQueue.queueUrl }

  fun createEvent(
    principal: String,
    what: String,
    detail: Map<String, Any?>,
  ) {
    auditSqsClient.sendMessage(
      SendMessageRequest.builder()
        .queueUrl(auditQueueUrl)
        .messageBody(
          objectMapper.writeValueAsString(
            HmppsAuditEvent(
              what = what,
              details = objectMapper.writeValueAsString(detail),
              who = principal,
            ),
          ),
        )
        .build(),
    )
  }
}
