package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.HmppsAuditEvent
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService

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
