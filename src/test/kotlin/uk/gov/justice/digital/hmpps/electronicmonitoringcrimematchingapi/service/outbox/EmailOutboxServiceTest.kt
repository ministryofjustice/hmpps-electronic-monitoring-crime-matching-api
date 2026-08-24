package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.outbox

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.EmailData
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.NamedDataSource
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.EmailIngestionOutcome
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.EmailOutbox
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.EmailOutboxStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.IngestionStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.outbox.EmailOutboxRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal.FeatureFlagService
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal.MetricsService
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Date
import java.util.Optional
import java.util.UUID

@ActiveProfiles("test")
class EmailOutboxServiceTest {
  private lateinit var repository: EmailOutboxRepository
  private lateinit var payloadMapper: EmailOutboxPayloadMapper
  private lateinit var featureFlagService: FeatureFlagService
  private lateinit var metricsService: MetricsService
  private lateinit var service: EmailOutboxService

  @BeforeEach
  fun setup() {
    repository = Mockito.mock(EmailOutboxRepository::class.java)
    payloadMapper = Mockito.mock(EmailOutboxPayloadMapper::class.java)
    featureFlagService = Mockito.mock(FeatureFlagService::class.java)
    metricsService = Mockito.mock(MetricsService::class.java)
    service = EmailOutboxService(
      repository,
      payloadMapper,
      featureFlagService,
      metricsService,
      Clock.fixed(Instant.parse("2026-08-24T10:15:30Z"), ZoneOffset.UTC),
    )

    whenever(payloadMapper.toJson(any(), any())).thenReturn("{}")
    whenever(repository.save(any<EmailOutbox>())).thenAnswer { it.arguments[0] as EmailOutbox }
    whenever(repository.transitionStatusIfCurrent(any(), any(), any(), anyOrNull(), any())).thenReturn(1)
  }

  private fun outcome(crimeBatchId: String, status: IngestionStatus) = EmailIngestionOutcome(
    crimeBatchId = crimeBatchId,
    emailData = EmailData(
      sender = "sender@police.gov.uk",
      originalSender = "officer@police.gov.uk",
      subject = "subject",
      sentAt = Date(),
      attachments = listOf(NamedDataSource("crimes.csv")),
    ),
    ingestionStatus = status,
  )

  @Test
  fun `it should enqueue a PENDING row per recipient with a parsed crime batch id`() {
    val crimeBatchId = UUID.randomUUID()
    whenever(featureFlagService.policeConfirmationEmailsEnabled()).thenReturn(true)

    service.enqueue(outcome(crimeBatchId.toString(), IngestionStatus.SUCCESSFUL))

    val captor = argumentCaptor<EmailOutbox>()
    verify(repository, times(2)).save(captor.capture())
    assertThat(captor.allValues).allSatisfy {
      assertThat(it.status).isEqualTo(EmailOutboxStatus.PENDING)
      assertThat(it.crimeBatchId).isEqualTo(crimeBatchId)
      assertThat(it.payload).isEqualTo("{}")
    }
    verify(metricsService, times(2)).recordOutboxEvent(EmailOutboxStatus.PENDING)
  }

  @Test
  fun `it should enqueue two PENDING rows with a null crime batch ids for failed outcomes`() {
    whenever(featureFlagService.policeConfirmationEmailsEnabled()).thenReturn(true)

    service.enqueue(outcome("Unknown due to an error", IngestionStatus.FAILED))

    val captor = argumentCaptor<EmailOutbox>()
    verify(repository, times(2)).save(captor.capture())
    assertThat(captor.firstValue.crimeBatchId).isNull()
  }

  @Test
  fun `it should not enqueue a PENDING row to the original sender when the send police email flag is false`() {
    val crimeBatchId = UUID.randomUUID()
    whenever(featureFlagService.policeConfirmationEmailsEnabled()).thenReturn(false)

    service.enqueue(outcome(crimeBatchId.toString(), IngestionStatus.SUCCESSFUL))

    val captor = argumentCaptor<EmailOutbox>()
    verify(repository, times(1)).save(captor.capture())
    assertThat(captor.allValues).allSatisfy {
      assertThat(it.status).isEqualTo(EmailOutboxStatus.PENDING)
      assertThat(it.crimeBatchId).isEqualTo(crimeBatchId)
      assertThat(it.payload).isEqualTo("{}")
    }
    verify(metricsService, times(1)).recordOutboxEvent(EmailOutboxStatus.PENDING)
  }

  @Test
  fun `it should mark an event as sent`() {
    val row = EmailOutbox(payload = "{}", status = EmailOutboxStatus.CLAIMED)

    service.markSent(row.eventId)

    verify(repository, times(1)).transitionStatusIfCurrent(
      eq(row.eventId),
      eq(EmailOutboxStatus.CLAIMED.name),
      eq(EmailOutboxStatus.SENT.name),
      isNull(),
      any(),
    )
    verify(metricsService, times(1)).recordOutboxEvent(EmailOutboxStatus.SENT)
  }

  @Test
  fun `it should increment attempts and record the error when marking dead`() {
    val row = EmailOutbox(payload = "{}", status = EmailOutboxStatus.CLAIMED, attempts = 2)

    service.markDead(row.eventId, "boom")

    verify(repository, times(1)).transitionStatusIfCurrent(
      eq(row.eventId),
      eq(EmailOutboxStatus.CLAIMED.name),
      eq(EmailOutboxStatus.DEAD.name),
      eq("boom"),
      any(),
    )
    verify(metricsService, times(1)).recordOutboxEvent(EmailOutboxStatus.DEAD)
  }

  @Test
  fun `it should mark an event as failed without further retries`() {
    val row = EmailOutbox(payload = "{}", status = EmailOutboxStatus.CLAIMED, attempts = 0)

    service.markFailed(row.eventId, "400 bad request")

    verify(repository, times(1)).transitionStatusIfCurrent(
      eq(row.eventId),
      eq(EmailOutboxStatus.CLAIMED.name),
      eq(EmailOutboxStatus.FAILED.name),
      eq("400 bad request"),
      any(),
    )
    verify(metricsService, times(1)).recordOutboxEvent(EmailOutboxStatus.FAILED)
  }

  @Test
  fun `it should keep the row claimed when marking retry`() {
    val row = EmailOutbox(payload = "{}", status = EmailOutboxStatus.CLAIMED)

    service.markRetry(row.eventId, "temporary failure")

    verify(repository, times(1)).transitionStatusIfCurrent(
      eq(row.eventId),
      eq(EmailOutboxStatus.CLAIMED.name),
      eq(EmailOutboxStatus.CLAIMED.name),
      eq("temporary failure"),
      any(),
    )
    verify(metricsService, times(1)).recordOutboxEvent(EmailOutboxStatus.CLAIMED)
  }

  @Test
  fun `it should not record metrics when a stale worker loses the transition race`() {
    val row = EmailOutbox(payload = "{}", status = EmailOutboxStatus.SENT)
    whenever(repository.transitionStatusIfCurrent(any(), any(), any(), anyOrNull(), any())).thenReturn(0)
    whenever(repository.findById(row.eventId)).thenReturn(Optional.of(row))

    service.markRetry(row.eventId, "boom")

    verify(repository, times(1)).transitionStatusIfCurrent(
      eq(row.eventId),
      eq(EmailOutboxStatus.CLAIMED.name),
      eq(EmailOutboxStatus.CLAIMED.name),
      eq("boom"),
      any(),
    )
    verify(metricsService, never()).recordOutboxEvent(any())
  }

  @Test
  fun `it should mark claimed rows when claiming a batch`() {
    val row = EmailOutbox(payload = "{}", status = EmailOutboxStatus.PENDING)
    whenever(repository.claimBatch(any(), eq(10))).thenReturn(listOf(row))

    val claimed = service.claimBatch(10)

    assertThat(claimed).hasSize(1)
    assertThat(row.status).isEqualTo(EmailOutboxStatus.CLAIMED)
    assertThat(row.claimedAt).isNotNull()
    verify(metricsService, times(1)).recordOutboxEvent(EmailOutboxStatus.CLAIMED)
  }
}
