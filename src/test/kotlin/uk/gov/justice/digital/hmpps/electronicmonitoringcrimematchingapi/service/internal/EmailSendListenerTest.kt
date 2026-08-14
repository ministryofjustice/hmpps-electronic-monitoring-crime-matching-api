package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.EmailData
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.NamedDataSource
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.EmailIngestionOutcome
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.EmailOutboxPayload
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.EmailSendMessage
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.EmailOutbox
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeBatchEmailIngestionErrorType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.EmailOutboxStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.IngestionStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.outbox.EmailOutboxPayloadMapper
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.outbox.EmailOutboxService
import uk.gov.service.notify.NotifyExceptions
import java.util.Date
import java.util.UUID

@ActiveProfiles("test")
class EmailSendListenerTest {
  private lateinit var emailOutboxService: EmailOutboxService
  private lateinit var emailOutboxPayloadMapper: EmailOutboxPayloadMapper
  private lateinit var emailNotificationService: EmailNotificationService
  private lateinit var listener: EmailSendListener

  @BeforeEach
  fun setup() {
    emailOutboxService = Mockito.mock(EmailOutboxService::class.java)
    emailOutboxPayloadMapper = Mockito.mock(EmailOutboxPayloadMapper::class.java)
    emailNotificationService = Mockito.mock(EmailNotificationService::class.java)
    listener = EmailSendListener(emailOutboxService, emailOutboxPayloadMapper, emailNotificationService, maxReceiveCount = 3)
  }

  private val recipient = "officer@police.gov.uk"

  private fun outcome() = EmailIngestionOutcome(
    emailData = EmailData(
      sender = recipient,
      originalSender = recipient,
      subject = "subject",
      sentAt = Date(),
      attachments = listOf(NamedDataSource("crimes.csv")),
    ),
    ingestionStatus = IngestionStatus.SUCCESSFUL,
  )

  private fun payload() = EmailOutboxPayload(
    ingestionStatus = IngestionStatus.SUCCESSFUL,
    recipient = recipient,
    fileName = "crimes.csv",
    batchId = "batchId",
    crimeBatchId = "142a9a57-337f-4208-908b-2874b75fa10d",
    policeForce = "Metropolitan",
    errorType = CrimeBatchEmailIngestionErrorType.UNKNOWN,
    recordCount = 0,
  )

  private fun row(status: EmailOutboxStatus): EmailOutbox = EmailOutbox(payload = "{}", status = status)

  private fun stubMapper(row: EmailOutbox) {
    val payload = payload()
    whenever(emailOutboxPayloadMapper.readPayload(row.payload)).thenReturn(payload)
    whenever(emailOutboxPayloadMapper.toOutcome(payload)).thenReturn(outcome())
  }

  @Test
  fun `it should send the email to the row recipient and mark the event as sent`() {
    val row = row(EmailOutboxStatus.CLAIMED)
    whenever(emailOutboxService.find(row.eventId)).thenReturn(row)
    stubMapper(row)

    listener.receiveEmailSend(EmailSendMessage(row.eventId), "1")

    verify(emailNotificationService, times(1)).sendEmail(any(), eq(recipient), eq(row.eventId.toString()))
    verify(emailOutboxService, times(1)).markSent(row.eventId)
  }

  @Test
  fun `it should not resend a terminal event`() {
    val row = row(EmailOutboxStatus.SENT)
    whenever(emailOutboxService.find(row.eventId)).thenReturn(row)

    listener.receiveEmailSend(EmailSendMessage(row.eventId), "1")

    verify(emailNotificationService, never()).sendEmail(any(), any(), any())
    verify(emailOutboxService, never()).markSent(any())
  }

  @Test
  fun `it should no-op for an unknown event`() {
    val eventId = UUID.randomUUID()
    whenever(emailOutboxService.find(eventId)).thenReturn(null)

    listener.receiveEmailSend(EmailSendMessage(eventId), "1")

    verify(emailNotificationService, never()).sendEmail(any(), any(), any())
  }

  @Test
  fun `it should mark retry and rethrow on a non-final failure`() {
    val row = row(EmailOutboxStatus.CLAIMED)
    whenever(emailOutboxService.find(row.eventId)).thenReturn(row)
    stubMapper(row)
    whenever(emailNotificationService.sendEmail(any(), any(), any())).thenThrow(RuntimeException("boom"))

    assertThrows<RuntimeException> {
      listener.receiveEmailSend(EmailSendMessage(row.eventId), "1")
    }

    verify(emailOutboxService, times(1)).markRetry(eq(row.eventId), any())
    verify(emailOutboxService, never()).markDead(any(), any())
  }

  @Test
  fun `it should mark dead and rethrow on the final failure`() {
    val row = row(EmailOutboxStatus.CLAIMED)
    whenever(emailOutboxService.find(row.eventId)).thenReturn(row)
    stubMapper(row)
    whenever(emailNotificationService.sendEmail(any(), any(), any())).thenThrow(RuntimeException("boom"))

    assertThrows<RuntimeException> {
      listener.receiveEmailSend(EmailSendMessage(row.eventId), "3")
    }

    verify(emailOutboxService, times(1)).markDead(eq(row.eventId), any())
    verify(emailOutboxService, never()).markRetry(any(), any())
  }

  @Test
  fun `it should mark failed without retrying on a permanent Notify failure`() {
    val row = row(EmailOutboxStatus.CLAIMED)
    whenever(emailOutboxService.find(row.eventId)).thenReturn(row)
    stubMapper(row)
    whenever(emailNotificationService.sendEmail(any(), any(), any())).thenAnswer { throw NotifyExceptions.withStatus(400, "bad request") }

    // No exception is re-thrown, so SQS deletes the message rather than retrying to the DLQ.
    listener.receiveEmailSend(EmailSendMessage(row.eventId), "1")

    verify(emailOutboxService, times(1)).markFailed(eq(row.eventId), any())
    verify(emailOutboxService, never()).markRetry(any(), any())
    verify(emailOutboxService, never()).markDead(any(), any())
  }

  @Test
  fun `it should retry a rate-limited Notify failure`() {
    val row = row(EmailOutboxStatus.CLAIMED)
    whenever(emailOutboxService.find(row.eventId)).thenReturn(row)
    stubMapper(row)
    whenever(emailNotificationService.sendEmail(any(), any(), any())).thenAnswer { throw NotifyExceptions.withStatus(429, "too many requests") }

    assertThrows<Exception> {
      listener.receiveEmailSend(EmailSendMessage(row.eventId), "1")
    }

    verify(emailOutboxService, times(1)).markRetry(eq(row.eventId), any())
    verify(emailOutboxService, never()).markFailed(any(), any())
  }
}
