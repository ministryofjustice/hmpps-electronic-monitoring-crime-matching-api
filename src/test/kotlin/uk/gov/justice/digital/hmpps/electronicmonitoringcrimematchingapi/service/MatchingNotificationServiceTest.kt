package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.test.context.ActiveProfiles
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sns.model.PublishResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.MatchingNotification
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.PublishMatchingOutbox
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PublishMatchingState
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.publishMatching.PublishMatchingOutboxRepository
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic
import java.time.Instant
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
  fun `it should send one crime matching request to the SNS topic`() {
    whenever(publishMatchingOutboxRepository.completeClaimedRow(any(), any(), any(), any(), any(), any())).thenReturn(1)
    whenever(hmppsQueueService.findByTopicId("matchingnotificationstopic")).thenReturn(HmppsTopic("id", "topicArn", snsClient))
    whenever(snsClient.publish(any<PublishRequest>())).thenReturn(completedFuture(PublishResponse.builder().messageId("1").build()))
    val batchId = UUID.randomUUID().toString()
    whenever(publishMatchingOutboxRepository.claimEligibleRows(eq(PublishMatchingState.PENDING.name), eq(PublishMatchingState.FAILED.name), any<Instant>(), any<Instant>(), any<Int>())).thenReturn(
      listOf(
        PublishMatchingOutbox(
          payload = mapper.writeValueAsString(
            MatchingNotification(
              type = MatchingNotificationService.CRIME_MATCHING_REQUEST,
              crimeBatchId = batchId,
            ),
          ),
          state = PublishMatchingState.PENDING,
          claimedAt = Instant.parse("2026-01-01T00:10:00Z"),
        ),
      ),
    )

    service.publishMatchingRequests()

    val captor = argumentCaptor<PublishRequest>()

    verify(hmppsQueueService, times(1)).findByTopicId(any<String>())
    verify(snsClient, times(1)).publish(captor.capture())

    assertThat(captor.allValues).hasSize(1)
    assertThat(captor.allValues.first().message()).isEqualTo("{\"type\":\"CRIME_MATCHING_REQUEST\",\"crime_batch_id\":\"${batchId}\"}")
  }

  @Test
  fun `it should send two crime matching requests to the SNS topic`() {
    whenever(publishMatchingOutboxRepository.completeClaimedRow(any(), any(), any(), any(), any(), any())).thenReturn(1)
    whenever(hmppsQueueService.findByTopicId("matchingnotificationstopic")).thenReturn(HmppsTopic("id", "topicArn", snsClient))
    whenever(snsClient.publish(any<PublishRequest>()))
      .thenReturn(completedFuture(PublishResponse.builder().messageId("1").build()))
      .thenReturn(completedFuture(PublishResponse.builder().messageId("2").build()))
    val batchId1 = UUID.randomUUID().toString()
    val batchId2 = UUID.randomUUID().toString()
    whenever(publishMatchingOutboxRepository.claimEligibleRows(eq(PublishMatchingState.PENDING.name), eq(PublishMatchingState.FAILED.name), any<Instant>(), any<Instant>(), any<Int>())).thenReturn(
      listOf(
        PublishMatchingOutbox(
          payload = mapper.writeValueAsString(
            MatchingNotification(
              type = MatchingNotificationService.CRIME_MATCHING_REQUEST,
              crimeBatchId = batchId1,
            ),
          ),
          state = PublishMatchingState.PENDING,
          claimedAt = Instant.parse("2026-01-01T00:10:00Z"),
        ),
        PublishMatchingOutbox(
          payload = mapper.writeValueAsString(
            MatchingNotification(
              type = MatchingNotificationService.CRIME_MATCHING_REQUEST,
              crimeBatchId = batchId2,
            ),
          ),
          state = PublishMatchingState.PENDING,
          claimedAt = Instant.parse("2026-01-01T00:10:00Z"),
        ),
      ),
    )

    service.publishMatchingRequests()

    val captor = argumentCaptor<PublishRequest>()

    verify(hmppsQueueService, times(1)).findByTopicId(any<String>())
    verify(snsClient, times(2)).publish(captor.capture())

    assertThat(captor.allValues).hasSize(2)
    assertThat(captor.allValues.map { it.message() }).containsExactly(
      "{\"type\":\"CRIME_MATCHING_REQUEST\",\"crime_batch_id\":\"${batchId1}\"}",
      "{\"type\":\"CRIME_MATCHING_REQUEST\",\"crime_batch_id\":\"${batchId2}\"}",
    )
  }

  @Test
  fun `it should save the state of the PublishMatchingRequest as FAILED if an error occurs`() {
    whenever(publishMatchingOutboxRepository.completeClaimedRow(any(), any(), any(), any(), any(), any())).thenReturn(1)
    whenever(hmppsQueueService.findByTopicId("matchingnotificationstopic")).thenReturn(HmppsTopic("id", "topicArn", snsClient))
    whenever(snsClient.publish(any<PublishRequest>())).thenThrow(RuntimeException("SNS error"))
    val batchId = UUID.randomUUID().toString()
    whenever(publishMatchingOutboxRepository.claimEligibleRows(eq(PublishMatchingState.PENDING.name), eq(PublishMatchingState.FAILED.name), any<Instant>(), any<Instant>(), any<Int>())).thenReturn(
      listOf(
        PublishMatchingOutbox(
          payload = mapper.writeValueAsString(
            MatchingNotification(
              type = MatchingNotificationService.CRIME_MATCHING_REQUEST,
              crimeBatchId = batchId,
            ),
          ),
          state = PublishMatchingState.PENDING,
          claimedAt = Instant.parse("2026-01-01T00:10:00Z"),
        ),
      ),
    )

    service.publishMatchingRequests()

    verify(hmppsQueueService, times(1)).findByTopicId(any<String>())
    verify(snsClient, atLeastOnce()).publish(any<PublishRequest>()) // triggers retry policy
    verify(publishMatchingOutboxRepository, times(1)).completeClaimedRow(any(), any(), eq(PublishMatchingState.FAILED.name), eq(1), any(), eq(0))
  }

  @Test
  fun `it should transition the state of the PublishMatchingRequest to DEAD if the max attempts is reached`() {
    whenever(publishMatchingOutboxRepository.completeClaimedRow(any(), any(), any(), any(), any(), any())).thenReturn(1)
    whenever(hmppsQueueService.findByTopicId("matchingnotificationstopic")).thenReturn(HmppsTopic("id", "topicArn", snsClient))
    whenever(snsClient.publish(any<PublishRequest>())).thenThrow(RuntimeException("SNS error"))
    val batchId = UUID.randomUUID().toString()
    whenever(publishMatchingOutboxRepository.claimEligibleRows(eq(PublishMatchingState.PENDING.name), eq(PublishMatchingState.FAILED.name), any<Instant>(), any<Instant>(), any<Int>())).thenReturn(
      listOf(
        PublishMatchingOutbox(
          payload = mapper.writeValueAsString(
            MatchingNotification(
              type = MatchingNotificationService.CRIME_MATCHING_REQUEST,
              crimeBatchId = batchId,
            ),
          ),
          attempts = MatchingNotificationService.MAX_PUBLISH_ATTEMPTS - 1,
          state = PublishMatchingState.PENDING,
          claimedAt = Instant.parse("2026-01-01T00:10:00Z"),
        ),
      ),
    )

    service.publishMatchingRequests()

    val stateCaptor = argumentCaptor<String>()
    val attemptsCaptor = argumentCaptor<Int>()

    verify(hmppsQueueService, times(1)).findByTopicId(any<String>())
    verify(snsClient, atLeastOnce()).publish(any<PublishRequest>()) // triggers retry policy
    verify(publishMatchingOutboxRepository, times(1)).completeClaimedRow(any(), any(), stateCaptor.capture(), attemptsCaptor.capture(), any(), eq(0))
    assertEquals(PublishMatchingState.DEAD.name, stateCaptor.firstValue)
    assertEquals(MatchingNotificationService.MAX_PUBLISH_ATTEMPTS, attemptsCaptor.firstValue)
  }

  @Test
  fun `it should update the state of the PublishMatchingRequest to PUBLISHED if the publish happens successfully`() {
    whenever(publishMatchingOutboxRepository.completeClaimedRow(any(), any(), any(), any(), any(), any())).thenReturn(1)
    whenever(hmppsQueueService.findByTopicId("matchingnotificationstopic")).thenReturn(HmppsTopic("id", "topicArn", snsClient))
    whenever(snsClient.publish(any<PublishRequest>())).thenReturn(completedFuture(PublishResponse.builder().messageId("1").build()))
    val batchId = UUID.randomUUID().toString()
    whenever(publishMatchingOutboxRepository.claimEligibleRows(eq(PublishMatchingState.PENDING.name), eq(PublishMatchingState.FAILED.name), any<Instant>(), any<Instant>(), any<Int>())).thenReturn(
      listOf(
        PublishMatchingOutbox(
          payload = mapper.writeValueAsString(
            MatchingNotification(
              type = MatchingNotificationService.CRIME_MATCHING_REQUEST,
              crimeBatchId = batchId,
            ),
          ),
          state = PublishMatchingState.PENDING,
          claimedAt = Instant.parse("2026-01-01T00:10:00Z"),
        ),
      ),
    )

    service.publishMatchingRequests()

    verify(hmppsQueueService, times(1)).findByTopicId(any<String>())
    verify(snsClient, times(1)).publish(any<PublishRequest>())
    verify(publishMatchingOutboxRepository, times(1)).completeClaimedRow(any(), any(), eq(PublishMatchingState.PUBLISHED.name), eq(1), eq(null), eq(0))
  }

  @Test
  fun `it should catch exception from objectMapper readValue and mark row as FAILED`() {
    val mockObjectMapper = Mockito.mock(ObjectMapper::class.java)
    val testException = RuntimeException("Failed to deserialize JSON")

    whenever(mockObjectMapper.readValue(any<String>(), any<Class<*>>())).thenThrow(testException)

    val serviceWithMockedMapper = MatchingNotificationService(
      hmppsQueueService = hmppsQueueService,
      objectMapper = mockObjectMapper,
      publishMatchingOutboxRepository = publishMatchingOutboxRepository,
    )

    val claimedAt = Instant.parse("2026-01-01T00:10:00Z")
    val publishMatchingOutbox = PublishMatchingOutbox(
      payload = """{"type":"CRIME_MATCHING_REQUEST","crime_batch_id":"test-batch"}""",
      state = PublishMatchingState.PENDING,
      claimedAt = claimedAt,
    )

    whenever(publishMatchingOutboxRepository.claimEligibleRows(eq(PublishMatchingState.PENDING.name), eq(PublishMatchingState.FAILED.name), any(), any(), any())).thenReturn(listOf(publishMatchingOutbox))
    whenever(publishMatchingOutboxRepository.completeClaimedRow(any(), any(), any(), any(), any(), any())).thenReturn(1)

    serviceWithMockedMapper.publishMatchingRequests()

    verify(publishMatchingOutboxRepository, times(1)).completeClaimedRow(
      id = publishMatchingOutbox.id,
      claimedAt = claimedAt,
      state = PublishMatchingState.FAILED.name,
      attempts = 1,
      lastError = "Failed to deserialize JSON",
      version = publishMatchingOutbox.version,
    )
  }
}
