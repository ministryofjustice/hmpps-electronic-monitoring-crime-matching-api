package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal

import jakarta.mail.util.ByteArrayDataSource
import org.json.JSONObject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config.notify.NotifyProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.EmailData
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.EmailIngestionOutcome
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeBatchEmailAttachmentIngestionErrorType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeBatchEmailIngestionErrorType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.IngestionStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.validation.EmailAttachmentIngestionError
import uk.gov.service.notify.NotificationClient
import java.time.Instant
import java.time.LocalDate
import java.util.Date

@ActiveProfiles("test")
class EmailNotificationServiceTest {
  private lateinit var service: EmailNotificationService
  private lateinit var notifyClient: NotificationClient
  private val notifyProperties: NotifyProperties = mock()

  @BeforeEach
  fun setup() {
    whenever(notifyProperties.successfulIngestionTemplateId).thenReturn("templateId")
    whenever(notifyProperties.failedIngestionTemplateId).thenReturn("failedTemplateId")
    whenever(notifyProperties.partialIngestionTemplateId).thenReturn("partialTemplateId")
    whenever(notifyProperties.errorIngestionTemplateId).thenReturn("errorTemplateId")
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
      attachments = listOf(attachment),
    )

    val uploadFile = JSONObject()

    val personalisation = mutableMapOf(
      "fileName" to "attachment.csv",
      "ingestionDate" to LocalDate.now().toString(),
      "batchId" to "batchId",
      "policeForce" to "BEDFORDSHIRE",
      "linkToFile" to uploadFile,
    )

    mockStatic(NotificationClient::class.java).use { staticMock ->
      staticMock
        .`when`<Any> {
          NotificationClient.prepareUpload(
            any(),
            any(),
          )
        }
        .thenReturn(uploadFile)

      val ingestionOutcome = EmailIngestionOutcome(
        batchId = "batchId",
        policeForce = "BEDFORDSHIRE",
        emailData = emailData,
        ingestionStatus = IngestionStatus.SUCCESSFUL,
      )

      service.sendEmails(ingestionOutcome)
    }

    verify(notifyClient, times(1)).sendEmail("templateId", "sender", personalisation, "batchId")
    verify(notifyClient, times(1)).sendEmail("templateId", "originalSender", personalisation, "batchId")
  }

  @Test
  fun `it should not send a successful ingestion email when notify is not enabled`() {
    val attachment = ByteArrayDataSource("data", "message/rfc822")
    attachment.name = "attachment.csv"
    val batchId = "batchId"

    val emailData = EmailData(
      sender = "sender",
      originalSender = "originalSender",
      subject = "subject",
      sentAt = Date.from(Instant.now()),
      attachments = listOf(attachment),
    )

    val personalisation = mutableMapOf(
      "fileName" to "attachment.csv",
      "ingestionDate" to LocalDate.now().toString(),
      "batchId" to batchId,
      "policeForce" to "BEDFORDSHIRE",
    )

    val ingestionOutcome = EmailIngestionOutcome(
      batchId = batchId,
      policeForce = PoliceForce.BEDFORDSHIRE.name,
      emailData = emailData,
      ingestionStatus = IngestionStatus.SUCCESSFUL,
    )

    assertDoesNotThrow { service.sendEmails(ingestionOutcome) }
    verify(notifyClient, times(0)).sendEmail("templateId", "sender", personalisation, batchId)
  }

  @Test
  fun `it should send a failed ingestion email when notify is enabled`() {
    whenever(notifyProperties.enabled).thenReturn(true)

    val emailData = EmailData(
      sender = "sender",
      originalSender = "originalSender",
      subject = "subject",
      sentAt = Date.from(Instant.now()),
      attachments = emptyList(),
    )

    val personalisation = mapOf(
      "fileName" to "Invalid File",
      "ingestionDate" to LocalDate.now().toString(),
      "batchId" to "Unknown due to an error",
      "policeForce" to "Unknown due to an error",
      "errorSummary" to CrimeBatchEmailIngestionErrorType.INVALID_ATTACHMENT.message,
      "totalCount" to 0,
    )

    val ingestionOutcome = EmailIngestionOutcome(
      batchId = "Unknown due to an error",
      policeForce = "Unknown due to an error",
      emailData = emailData,
      errorType = CrimeBatchEmailIngestionErrorType.INVALID_ATTACHMENT,
      ingestionStatus = IngestionStatus.FAILED,
    )

    assertDoesNotThrow { service.sendEmails(ingestionOutcome) }

    verify(notifyClient, times(1)).sendEmail("failedTemplateId", "sender", personalisation, "Unknown due to an error")
    verify(notifyClient, times(1)).sendEmail("failedTemplateId", "sender", personalisation, "Unknown due to an error")
  }

  @Test
  fun `it should send a partial ingestion email with errorSummary and CSV attachment when notify is enabled`() {
    whenever(notifyProperties.enabled).thenReturn(true)

    val errors = (1..7).map { i ->
      EmailAttachmentIngestionError(
        rowNumber = i.toLong(),
        crimeReference = "CRI0000000$i",
        crimeTypeId = null,
        errorType = CrimeBatchEmailAttachmentIngestionErrorType.INVALID_ENUM,
        field = "crimeTypeId",
        value = "INVALID_$i",
      )
    }
    val attachment = ByteArrayDataSource("data", "message/rfc822")
    attachment.name = "attachment.csv"

    val emailData = EmailData(
      sender = "sender",
      originalSender = "originalSender",
      subject = "subject",
      sentAt = Date.from(Instant.now()),
      attachments = listOf(attachment),
    )

    val uploadFile = JSONObject()

    val batchId = "batchId"

    mockStatic(NotificationClient::class.java).use { staticMock ->
      staticMock
        .`when`<Any> {
          NotificationClient.prepareUpload(
            any(),
            any(),
          )
        }
        .thenReturn(uploadFile)

      val ingestionOutcome = EmailIngestionOutcome(
        batchId = batchId,
        policeForce = PoliceForce.METROPOLITAN.name,
        emailData = emailData,
        errors = errors,
        ingestionStatus = IngestionStatus.PARTIAL,
        recordCount = 10,
      )

      service.sendEmails(ingestionOutcome)
      staticMock.verify({ NotificationClient.prepareUpload(any(), any()) }, times(1))

      verify(notifyClient, times(1)).sendEmail(eq("partialTemplateId"), eq("sender"), any(), eq(batchId))
      verify(notifyClient, times(1)).sendEmail(eq("partialTemplateId"), eq("originalSender"), any(), eq(batchId))
    }
  }

    @Test
    fun `it should send an error ingestion email with errorSummary and CSV attachment when notify is enabled`() {
      whenever(notifyProperties.enabled).thenReturn(true)

      val errors = listOf(
        EmailAttachmentIngestionError(
          rowNumber = 1,
          crimeReference = "CRI00000001",
          crimeTypeId = null,
          errorType = CrimeBatchEmailAttachmentIngestionErrorType.INVALID_ENUM,
          field = "crimeTypeId",
          value = "",
        )
      )
      val attachment = ByteArrayDataSource("data", "message/rfc822")
      attachment.name = "attachment.csv"

      val emailData = EmailData(
        sender = "sender",
        originalSender = "originalSender",
        subject = "subject",
        sentAt = Date.from(Instant.now()),
        attachments = listOf(attachment),
      )

      val uploadFile = JSONObject()

      val batchId = "batchId"

      mockStatic(NotificationClient::class.java).use { staticMock ->
        staticMock
          .`when`<Any> {
            NotificationClient.prepareUpload(
              any(),
              any(),
            )
          }
          .thenReturn(uploadFile)

        val ingestionOutcome = EmailIngestionOutcome(
          batchId = batchId,
          policeForce = PoliceForce.METROPOLITAN.name,
          emailData = emailData,
          errors = errors,
          ingestionStatus = IngestionStatus.ERROR,
          recordCount = 1,
        )

        service.sendEmails(ingestionOutcome)
        staticMock.verify({ NotificationClient.prepareUpload(any(), any()) }, times(1))

        verify(notifyClient, times(1)).sendEmail(eq("errorTemplateId"), eq("sender"), any(), eq(batchId))
        verify(notifyClient, times(1)).sendEmail(eq("errorTemplateId"), eq("originalSender"), any(), eq(batchId))
      }
    }
}
