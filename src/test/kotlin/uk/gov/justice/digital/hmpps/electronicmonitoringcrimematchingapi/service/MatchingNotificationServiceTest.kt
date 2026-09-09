package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.test.context.ActiveProfiles
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sns.model.PublishResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.exception.PublishEventException
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.MatchingNotification
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchIngestionAttempt
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.PublishMatchingOutbox
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PublishMatchingState
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.publishMatching.PublishMatchingOutboxRepository
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic
import java.util.UUID
import java.util.concurrent.CompletableFuture.completedFuture

@ActiveProfiles("test")
class MatchingNotificationServiceTest {
  private lateinit var service: MatchingNotificationService
  private lateinit var hmppsQueueService: HmppsQueueService
  private lateinit var snsClient: SnsAsyncClient
  private lateinit var publishMatchingOutboxRepository: PublishMatchingOutboxRepository
  private val mapper: ObjectMapper = jacksonObjectMapper()

  @BeforeEach
  fun setup() {
    hmppsQueueService = Mockito.mock(HmppsQueueService::class.java)
    snsClient = Mockito.mock(SnsAsyncClient::class.java)
    publishMatchingOutboxRepository = Mockito.mock(PublishMatchingOutboxRepository::class.java)
    service = MatchingNotificationService(
      hmppsQueueService = hmppsQueueService,
      objectMapper = mapper,
      publishMatchingOutboxRepository = publishMatchingOutboxRepository,
    )
  }

  @Test
  fun `it should send a crime matching request to the SNS topic`() {
    whenever(publishMatchingOutboxRepository.save(any<PublishMatchingOutbox>())).thenAnswer { it.arguments[0] }
    whenever(hmppsQueueService.findByTopicId("matchingnotificationstopic")).thenReturn(HmppsTopic("id", "topicArn", snsClient))
    whenever(snsClient.publish(any<PublishRequest>())).thenReturn(completedFuture(PublishResponse.builder().messageId("1").build()))
    val batchId = UUID.randomUUID().toString()
    val ingestionAttempt = CrimeBatchIngestionAttempt(
      bucket = "bucket",
      objectName = "objectName",
    )

    service.publishMatchingRequest(batchId)

    val captor = argumentCaptor<PublishRequest>()

    verify(hmppsQueueService, times(1)).findByTopicId(any<String>())
    verify(snsClient, times(1)).publish(captor.capture())

    assertThat(captor.allValues).hasSize(1)
    assertThat(captor.allValues.first().message()).isEqualTo("{\"type\":\"CRIME_MATCHING_REQUEST\",\"crime_batch_id\":\"${batchId}\"}")
  }

  @Test
  fun `it does not save the state of the PublishMatchingRequest if an error occurs`() {
    whenever(publishMatchingOutboxRepository.save(any<PublishMatchingOutbox>())).thenAnswer { it.arguments[0] }
    whenever(hmppsQueueService.findByTopicId("matchingnotificationstopic")).thenReturn(HmppsTopic("id", "topicArn", snsClient))
    whenever(snsClient.publish(any<PublishRequest>())).thenThrow(RuntimeException("SNS error"))
    val batchId = UUID.randomUUID().toString()
    val ingestionAttempt = CrimeBatchIngestionAttempt(
      bucket = "bucket",
      objectName = "objectName",
    )

    assertThrows<PublishEventException> { service.publishMatchingRequest(batchId) }

    verify(hmppsQueueService, times(1)).findByTopicId(any<String>())
    verify(snsClient, atLeastOnce()).publish(any<PublishRequest>()) // triggers retry policy
    verify(publishMatchingOutboxRepository, times(0)).save(any())
  }

  @Test
  fun `it should update the state of the PublishMatchingRequest to PUBLISHED if the publish happens successfully`() {
    whenever(hmppsQueueService.findByTopicId("matchingnotificationstopic")).thenReturn(HmppsTopic("id", "topicArn", snsClient))
    whenever(snsClient.publish(any<PublishRequest>())).thenReturn(completedFuture(PublishResponse.builder().messageId("1").build()))
    val batchId = UUID.randomUUID().toString()
    val ingestionAttempt = CrimeBatchIngestionAttempt(
      bucket = "bucket",
      objectName = "objectName",
    )
    whenever(publishMatchingOutboxRepository.findAllByPayload(any<String>())).thenReturn(
      listOf(
        PublishMatchingOutbox(
          payload = mapper.writeValueAsString(
            MatchingNotification(
              type = MatchingNotificationService.CRIME_MATCHING_REQUEST,
              crimeBatchId = batchId,
            ),
          ),
          state = PublishMatchingState.PENDING_OR_UNCONFIRMED,
        ),
      ),
    )

    service.publishMatchingRequest(batchId)

    val captor = argumentCaptor<PublishMatchingOutbox>()

    verify(hmppsQueueService, times(1)).findByTopicId(any<String>())
    verify(snsClient, times(1)).publish(any<PublishRequest>())
    verify(publishMatchingOutboxRepository, times(1)).save(captor.capture())

    assertThat(captor.allValues).hasSize(1)
    assertThat(captor.allValues.first().state).isEqualTo(PublishMatchingState.PUBLISHED)
  }
}
