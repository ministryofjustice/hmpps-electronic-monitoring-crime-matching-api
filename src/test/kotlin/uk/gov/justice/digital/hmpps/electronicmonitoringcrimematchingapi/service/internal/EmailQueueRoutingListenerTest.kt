package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.EmailSendMessage
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.SqsMessage
import java.util.UUID

class EmailQueueRoutingListenerTest {
  private lateinit var emailListener: EmailListener
  private lateinit var emailSendListener: EmailSendListener
  private lateinit var router: EmailQueueRoutingListener

  @BeforeEach
  fun setUp() {
    emailListener = Mockito.mock(EmailListener::class.java)
    emailSendListener = Mockito.mock(EmailSendListener::class.java)
    router = EmailQueueRoutingListener(jacksonObjectMapper(), emailListener, emailSendListener)
  }

  @Test
  fun `it routes SNS-wrapped ingestion messages to EmailListener`() {
    val messageId = UUID.randomUUID()
    val rawMessage =
      """
      {
        "Type": "Notification",
        "Message": "{\"receipt\":{\"action\":{\"bucketName\":\"emails\",\"objectKey\":\"email-file\"}}}",
        "MessageId": "$messageId"
      }
      """.trimIndent()

    router.route(rawMessage, "1")

    val sqsMessageCaptor = argumentCaptor<SqsMessage>()
    verify(emailListener, times(1)).receiveEmailNotification(sqsMessageCaptor.capture())
    verify(emailSendListener, never()).receiveEmailSend(org.mockito.kotlin.any(), org.mockito.kotlin.anyOrNull())
    assertThat(sqsMessageCaptor.firstValue.MessageId).isEqualTo(messageId)
  }

  @Test
  fun `it routes emailsend messages to EmailSendListener`() {
    val eventId = UUID.randomUUID()
    val rawMessage = """{"eventId":"$eventId","messageType":"EMAILSEND"}"""

    router.route(rawMessage, "2")

    val emailSendCaptor = argumentCaptor<EmailSendMessage>()
    verify(emailSendListener, times(1)).receiveEmailSend(emailSendCaptor.capture(), eq("2"))
    verify(emailListener, never()).receiveEmailNotification(org.mockito.kotlin.any())
    assertThat(emailSendCaptor.firstValue.eventId).isEqualTo(eventId)
  }

  @Test
  fun `it rejects messages with no messageType and no SNS Message field`() {
    val ex = assertThrows<IllegalArgumentException> {
      router.route("""{"unexpected":"payload"}""", "1")
    }
    assertThat(ex.message).contains("Unsupported message schema")
  }

  @Test
  fun `it routes messages with an unrecognised messageType value to the ingestion path when SNS Message field is present`() {
    // An unknown enum value should fall through to null branch; if Message is present it routes to ingestion.
    val messageId = UUID.randomUUID()
    val rawMessage = """{"Type":"Notification","Message":"{}","MessageId":"$messageId","messageType":"FUTURE_TYPE"}"""

    // Should not throw — routes to emailListener (Message field present, messageType unrecognised → null branch)
    router.route(rawMessage, "1")
    verify(emailListener, times(1)).receiveEmailNotification(org.mockito.kotlin.any())
  }
}
