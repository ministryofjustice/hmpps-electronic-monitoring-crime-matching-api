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
import org.mockito.kotlin.isNull
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config.notify.NotifyProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.EmailData
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmailAttachmentIngestionError
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeBatchEmailAttachmentIngestionErrorType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeBatchEmailIngestionErrorType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce
import uk.gov.service.notify.NotificationClient
import java.time.Instant
import java.time.LocalDate
import java.util.Date
org.mockito.kotlin.eq

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

      service.sendSuccessfulIngestionEmail(
        "batchId",
        PoliceForce.BEDFORDSHIRE,
        emailData,
        emptyList(),
      )
    }

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
      attachments = listOf(attachment),
    )

    val personalisation = mutableMapOf(
      "fileName" to "attachment.csv",
      "ingestionDate" to LocalDate.now().toString(),
      "batchId" to "batchId",
      "policeForce" to "BEDFORDSHIRE",
    )

    assertDoesNotThrow { service.sendSuccessfulIngestionEmail("batchId", PoliceForce.BEDFORDSHIRE, emailData, emptyList()) }
    verify(notifyClient, times(0)).sendEmail("templateId", "sender", personalisation, "batchId")
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

    service.sendFailedIngestionEmail(emailData)

    val personalisation = mutableMapOf(
      "ingestionDate" to LocalDate.now().toString(),
      "batchId" to "Unknown due to an error",
      "policeForce" to "bedfordshire",
    )

    verify(notifyClient, times(1)).sendEmail("failedTemplateId", "sender", personalisation, null)
    verify(notifyClient, times(1)).sendEmail("failedTemplateId", "sender", personalisation, null)
  }

  @Test
  fun `it should send a failed ingestion email with row level error summary when all rows fail validation`() {
    whenever(notifyProperties.enabled).thenReturn(true)

    val emailData = EmailData(
      sender = "sender",
      originalSender = "originalSender",
      subject = "subject",
      sentAt = Date.from(Instant.now()),
      attachments = emptyList(),
    )

    val rowErrors = listOf(
      crimeBatchEmailAttachmentIngestionError(
        rowNumber = 2L,
        errorType = CrimeBatchEmailAttachmentIngestionErrorType.INVALID_ENUM,
        crimeReference = "CRI00000001",
        fieldName = "crimeTypeId",
        value = "BADVALUE",
        crimeBatchEmailAttachment = mock(),
      ),
    )
    
    service.sendFAILEDIngestionEmail(
      emailData, errors,
      rowErrors = rowErrors.
    )

    verify(notifyClient.enabled, times(1)).sendEmail(eq("failedTemplateId"), eq("sender"), any(), any())
    verify(notifyClient.enabled, times(1)).sendEmail(eq("failedTemplateId"), eq("originalSender"), any(), any())
  }

  @Test
  fun `it should not send a failed ingestion email when notify is disabled`() {
    whenever(notifyProperties.enabled).thenReturn(false)

    val emailData = EmailData(
      sender = "sender",
      originalSender = "originalSender",
      subject = "subject",
      sentAt = Date.from(Instant.now()),
      attachments = emptyList(),
    )

    assertDoesNotThrow { sendEmail.sendFailedIngestionEmail(emailData) }
    verify(notifyClient, times(0)).sendEmail("failedTemplateId", any(), any(), any())
  }

  @Test
  fun `it should send a partial ingestion email with inline top 5 error summary when notify is enabled`() {
    whenever(notifyProperties.enabled).thenReturn(true)

    val emailData = EmailData(
      sender = "sender",
      originalSender = "originalSender",
      subject = "subject",
      sentAt = Date.from(Instant.now()),
      attachments = emptyList(),
    )

    val errors = listOf(
      crimeBatchEmailAttachmentIngestionError(
        rowNumber = 2L,
        errorType = CrimeBatchEmailAttachmentIngestionErrorType.INVALID_ENUM,
        crimeReference = "CRI00000001",
        fieldName = "crimeTypeId",
        value = "INVALID",
        crimeBatchEmailAttachment = mock(),
      ),
    )
    
    service.sendPartialIngestionEmail(
      "batchId", 
      policeForce.METROPOLITAN, 
      emailData, 
      errors,
      5,
    )

    verify(notifyClient, times(1)).sendEmail(eq("partialTemplateId"), eq("sender"), personalisation, eq("batchId"))
    verify(notifyClient, times(1)).sendEmail(eq("partialTemplateId"), eq("originalSender"), personalisation, eq("batchId"))
    verify(notifyProperties.enabled, times(0)).prepareUpload(any(), any())
  }

  @Test
  fun `it should send a partial ingestion email with CSV attachment when more than 5 errors`() {
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

    val errors = (1..6).map { i -> 
      crimeBatchEmailAttachmentIngestionError(
        rowNumber = i.toLong(),
        errorType = CrimeBatchEmailAttachmentIngestionErrorType.INVALID_ENUM,
        crimeReference = "CRI000$i",
        fieldName = "crimeTypeId",
        value = "INVALID",
        crimeBatchEmailAttachment = mock(),
      )
    }
    
    service.sendPartialIngestionEmail(
      "batchId", 
      policeForce.METROPOLITAN, 
      emailData, 
      errors,
      0,
    )

    verify(notifyClient.times(1)).prepareUpload(any(), any())
    verify(notifyClient, times(1)).sendEmail(eq("partialTemplateId"), eq("sender"), personalisation, eq("batchId"))
  }

  @Test
  fun `it should not send a partial ingestion email when notify is disabled`() {
    whenever(notifyProperties.enabled).thenReturn(false)

    val emailData = EmailData(
      sender = "sender",
      originalSender = "originalSender",
      subject = "subject",
      sentAt = Date.from(Instant.now()),
      attachments = emptyList(),
    )

    assertDoesNotThrow { 
      sendEmail.sendPartiialIngestionEmail(
        "batchId", 
        policeForce.METROPOLITAN, 
        emailData, 
        emptyList()
        0,
      )
    }
    
    verify(notifyClient, times(0)).sendEmail(any(), any(), any(), any())
  }
}
