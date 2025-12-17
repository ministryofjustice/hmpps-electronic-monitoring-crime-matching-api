package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.test.context.ActiveProfiles
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sns.model.PublishResponse
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic
import java.util.UUID
import java.util.concurrent.CompletableFuture.completedFuture

@ActiveProfiles("test")
class MatchingNotificationServiceTest {
  private lateinit var service: MatchingNotificationService
  private lateinit var hmppsQueueService: HmppsQueueService
  private lateinit var snsClient: SnsAsyncClient
  private val mapper: ObjectMapper = jacksonObjectMapper()

  @BeforeEach
  fun setup() {
    hmppsQueueService = Mockito.mock(HmppsQueueService::class.java)
    snsClient = Mockito.mock(SnsAsyncClient::class.java)
    service = MatchingNotificationService(
      hmppsQueueService = hmppsQueueService,
      objectMapper = mapper,
    )
  }

  @Test
  fun `it should send a crime matching request to the SNS topic`() {
    whenever(hmppsQueueService.findByTopicId("matchingnotificationstopic")).thenReturn(HmppsTopic("id", "topicArn", snsClient))
    whenever(snsClient.publish(any<PublishRequest>())).thenReturn(completedFuture(PublishResponse.builder().messageId("1").build()))
    val batchId = UUID.randomUUID().toString()

    service.publishMatchingRequest(batchId)

    val captor = argumentCaptor<PublishRequest>()

    verify(hmppsQueueService, times(1)).findByTopicId(any<String>())
    verify(snsClient, times(1)).publish(captor.capture())

    assertThat(captor.allValues).hasSize(1)
    assertThat(captor.allValues.first().message()).isEqualTo("{\"type\":\"CRIME_MATCHING_REQUEST\",\"crime_batch_id\":\"${batchId}\"}")
  }
}
