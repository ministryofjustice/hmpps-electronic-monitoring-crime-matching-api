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
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchIngestionAttempt
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.PublishMatchingOutbox
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.IngestionStatus
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

    service.publishMatchingRequestIfRequired(batchId, IngestionStatus.SUCCESSFUL, ingestionAttempt)

    val captor = argumentCaptor<PublishRequest>()

    verify(hmppsQueueService, times(1)).findByTopicId(any<String>())
    verify(snsClient, times(1)).publish(captor.capture())

    assertThat(captor.allValues).hasSize(1)
    assertThat(captor.allValues.first().message()).isEqualTo("{\"type\":\"CRIME_MATCHING_REQUEST\",\"crime_batch_id\":\"${batchId}\"}")
  }

  @Test
  fun `it should leave the state of the PublishMatchingRequest as PENDING_OR_UNCONFIRMED if an error occurs`() {
    whenever(publishMatchingOutboxRepository.save(any<PublishMatchingOutbox>())).thenAnswer { it.arguments[0] }
    whenever(hmppsQueueService.findByTopicId("matchingnotificationstopic")).thenReturn(HmppsTopic("id", "topicArn", snsClient))
    whenever(snsClient.publish(any<PublishRequest>())).thenThrow(RuntimeException("SNS error"))
    val batchId = UUID.randomUUID().toString()
    val ingestionAttempt = CrimeBatchIngestionAttempt(
      bucket = "bucket",
      objectName = "objectName",
    )

    assertThrows<PublishEventException> { service.publishMatchingRequestIfRequired(batchId, IngestionStatus.SUCCESSFUL, ingestionAttempt) }

    val captor = argumentCaptor<PublishMatchingOutbox>()

    verify(hmppsQueueService, times(1)).findByTopicId(any<String>())
    verify(snsClient, atLeastOnce()).publish(any<PublishRequest>()) // triggers retry policy
    verify(publishMatchingOutboxRepository, times(1)).save(captor.capture())

    assertThat(captor.allValues).hasSize(1)
    assertThat(captor.allValues.first().state).isEqualTo(PublishMatchingState.PENDING_OR_UNCONFIRMED)
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
    whenever(publishMatchingOutboxRepository.save(any<PublishMatchingOutbox>())).thenAnswer {
      PublishMatchingOutbox(
        id = UUID.randomUUID(),
        crimeBatchIngestionAttempt = ingestionAttempt,
        state = (it.arguments[0] as PublishMatchingOutbox).state,
      )
    }

    service.publishMatchingRequestIfRequired(batchId, IngestionStatus.SUCCESSFUL, ingestionAttempt)

    val captor = argumentCaptor<PublishMatchingOutbox>()

    verify(hmppsQueueService, times(1)).findByTopicId(any<String>())
    verify(snsClient, times(1)).publish(any<PublishRequest>())
    verify(publishMatchingOutboxRepository, times(2)).save(captor.capture())

    assertThat(captor.allValues).hasSize(2)
    assertThat(captor.allValues.first().state).isEqualTo(PublishMatchingState.PENDING_OR_UNCONFIRMED)
    assertThat(captor.allValues.last().state).isEqualTo(PublishMatchingState.PUBLISHED)
  }

  @Test
  fun `it should not send a crime matching request to the SNS topic if the ingestion was not successful`() {
    whenever(publishMatchingOutboxRepository.save(any<PublishMatchingOutbox>())).thenAnswer { it.arguments[0] }
    whenever(hmppsQueueService.findByTopicId("matchingnotificationstopic")).thenReturn(HmppsTopic("id", "topicArn", snsClient))
    whenever(snsClient.publish(any<PublishRequest>())).thenReturn(completedFuture(PublishResponse.builder().messageId("1").build()))
    val batchId = UUID.randomUUID().toString()
    val ingestionAttempt = CrimeBatchIngestionAttempt(
      bucket = "bucket",
      objectName = "objectName",
    )

    service.publishMatchingRequestIfRequired(batchId, IngestionStatus.FAILED, ingestionAttempt)

    val captor = argumentCaptor<PublishMatchingOutbox>()

    verify(hmppsQueueService, times(0)).findByTopicId(any<String>())
    verify(snsClient, times(0)).publish(any<PublishRequest>())
    verify(publishMatchingOutboxRepository, times(1)).save(captor.capture())

    assertThat(captor.allValues).hasSize(1)
    assertThat(captor.allValues.first().state).isEqualTo(PublishMatchingState.NOT_REQUIRED)
  }
}
