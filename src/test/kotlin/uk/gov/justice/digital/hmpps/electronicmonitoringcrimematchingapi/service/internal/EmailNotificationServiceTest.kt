package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config.notify.NotifyProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.EmailData
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce
import uk.gov.service.notify.NotificationClient
import java.time.Instant
import java.time.LocalDate
import java.util.Date
import javax.mail.util.ByteArrayDataSource

@ActiveProfiles("test")
class EmailNotificationServiceTest {
  private lateinit var service: EmailNotificationService
  private lateinit var notifyClient: NotificationClient
  private val notifyProperties: NotifyProperties = mock()

  @BeforeEach
  fun setup() {
    whenever(notifyProperties.successfulIngestionTemplateId).thenReturn("templateId")
    notifyClient = Mockito.mock(NotificationClient::class.java)
    service = EmailNotificationService(notifyClient, notifyProperties)
  }

  @Test
  fun `it should send a successful ingestion email when notify is enabled`() {
    whenever(notifyProperties.enabled).thenReturn(true)

    val attachment = ByteArrayDataSource("data", "message/rfc822")
    attachment.name = "attachment.csv"

    val emailData = EmailData(
      sender = "sender",
      originalSender = "originalSender",
      subject = "subject",
      sentAt = Date.from(Instant.now()),
      attachment = attachment,
    )

    val personalisation = mutableMapOf<String, Any>(
      "fileName" to "attachment.csv",
      "ingestionDate" to LocalDate.now().toString(),
      "batchId" to "batchId",
      "policeForce" to "BEDFORDSHIRE",
    )

    assertDoesNotThrow { service.sendSuccessfulIngestionEmail("batchId", PoliceForce.BEDFORDSHIRE, emailData) }
    verify(notifyClient, times(1)).sendEmail("templateId", "sender", personalisation, "batchId")
    verify(notifyClient, times(1)).sendEmail("templateId", "originalSender", personalisation, "batchId")
  }

  @Test
  fun `it should not send a successful ingestion email when notify is not enabled`() {
    val attachment = ByteArrayDataSource("data", "message/rfc822")
    attachment.name = "attachment.csv"

    val emailData = EmailData(
      sender = "sender",
      originalSender = "originalSender",
      subject = "subject",
      sentAt = Date.from(Instant.now()),
      attachment = attachment,
    )

    val personalisation = mutableMapOf<String, Any>(
      "fileName" to "attachment.csv",
      "ingestionDate" to LocalDate.now().toString(),
      "batchId" to "batchId",
      "policeForce" to "BEDFORDSHIRE",
    )

    assertDoesNotThrow { service.sendSuccessfulIngestionEmail("batchId", PoliceForce.BEDFORDSHIRE, emailData) }
    verify(notifyClient, times(0)).sendEmail("templateId", "sender", personalisation, "batchId")
  }
}
