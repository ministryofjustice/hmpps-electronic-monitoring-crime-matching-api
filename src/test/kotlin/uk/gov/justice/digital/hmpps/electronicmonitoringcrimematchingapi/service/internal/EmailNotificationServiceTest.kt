package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.mail.util.ByteArrayDataSource
import org.json.JSONObject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config.notify.NotifyProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.EmailData
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.EmailIngestionOutcome
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.EmailOutbox
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeBatchEmailAttachmentIngestionErrorType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeBatchEmailIngestionErrorType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.EmailOutboxState
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.IngestionStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.validation.EmailAttachmentIngestionError
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.notifyEmailing.EmailOutboxRepository
import uk.gov.service.notify.NotificationClient
import java.time.Instant
import java.time.ZoneOffset
import java.util.Date

@ActiveProfiles("test")
class EmailNotificationServiceTest {
  private lateinit var service: EmailNotificationService
  private lateinit var notifyClient: NotificationClient
  private lateinit var emailOutboxRepository: EmailOutboxRepository
  private val mapper: ObjectMapper = jacksonObjectMapper()
  private val notifyProperties: NotifyProperties = mock()
  private val featureFlagService: FeatureFlagService = mock()
  private val utcToday: String = Instant.now().atZone(ZoneOffset.UTC).toLocalDate().toString()

  @BeforeEach
  fun setup() {
    whenever(notifyProperties.successfulIngestionTemplateId).thenReturn("templateId")
    whenever(notifyProperties.failedIngestionTemplateId).thenReturn("failedTemplateId")
    whenever(notifyProperties.partialIngestionTemplateId).thenReturn("partialTemplateId")
    whenever(notifyProperties.errorIngestionTemplateId).thenReturn("errorTemplateId")
    whenever(featureFlagService.policeConfirmationEmailsEnabled()).thenReturn(true)
    notifyClient = Mockito.mock(NotificationClient::class.java)
    emailOutboxRepository = Mockito.mock(EmailOutboxRepository::class.java)
    service = EmailNotificationService(featureFlagService, notifyClient, notifyProperties, emailOutboxRepository, mapper)
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
      "ingestionDate" to utcToday,
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

      val outboxCaptor = argumentCaptor<EmailOutbox>()

      service.createEmailOutboxRequest(ingestionOutcome)
      verify(emailOutboxRepository, times(2)).save(outboxCaptor.capture())
      val claimedRows = outboxCaptor.allValues
      claimedRows.forEach { row -> row.claimedAt = Instant.now() }
      whenever(emailOutboxRepository.claimEligibleRows(eq(EmailOutboxState.PENDING.name), any(), any())).thenReturn(claimedRows)

      service.sendEmails()
    }

    verify(notifyClient, times(1)).sendEmail("templateId", "sender", personalisation, "batchId")
    verify(notifyClient, times(1)).sendEmail("templateId", "originalSender", personalisation, "batchId")
  }

  @Test
  fun `it should not queue an outbox row for a successful ingestion email when notify is not enabled`() {
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
      "ingestionDate" to utcToday,
      "batchId" to batchId,
      "policeForce" to "BEDFORDSHIRE",
    )

    val ingestionOutcome = EmailIngestionOutcome(
      batchId = "batchId",
      policeForce = "BEDFORDSHIRE",
      emailData = emailData,
      ingestionStatus = IngestionStatus.SUCCESSFUL,
    )

    assertDoesNotThrow {
      val outboxCaptor = argumentCaptor<EmailOutbox>()

      service.createEmailOutboxRequest(ingestionOutcome)
      verify(emailOutboxRepository, times(0)).save(outboxCaptor.capture())
    }
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
      "ingestionDate" to utcToday,
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

    assertDoesNotThrow {
      val outboxCaptor = argumentCaptor<EmailOutbox>()

      service.createEmailOutboxRequest(ingestionOutcome)
      verify(emailOutboxRepository, times(2)).save(outboxCaptor.capture())
      val claimedRows = outboxCaptor.allValues
      claimedRows.forEach { row -> row.claimedAt = Instant.now() }
      whenever(emailOutboxRepository.claimEligibleRows(eq(EmailOutboxState.PENDING.name), any(), any())).thenReturn(claimedRows)

      service.sendEmails()
    }

    verify(notifyClient, times(1)).sendEmail("failedTemplateId", "sender", personalisation, "Unknown due to an error")
    verify(notifyClient, times(1)).sendEmail("failedTemplateId", "originalSender", personalisation, "Unknown due to an error")
  }

  @Test
  fun `it should send a partial ingestion email with errorSummary and CSV attachment when notify is enabled`() {
    whenever(notifyProperties.enabled).thenReturn(true)

    val errors = (1..7).map { i ->
      EmailAttachmentIngestionError(
        rowNumber = i.toLong(),
        crimeReference = "CRI0000000$i",
        crimeTypeId = null,
        errorType = CrimeBatchEmailAttachmentIngestionErrorType.INVALID_CRIME_TYPE,
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

      val outboxCaptor = argumentCaptor<EmailOutbox>()

      service.createEmailOutboxRequest(ingestionOutcome)
      verify(emailOutboxRepository, times(2)).save(outboxCaptor.capture())
      val claimedRows = outboxCaptor.allValues
      claimedRows.forEach { row -> row.claimedAt = Instant.now() }
      whenever(emailOutboxRepository.claimEligibleRows(eq(EmailOutboxState.PENDING.name), any(), any())).thenReturn(claimedRows)

      service.sendEmails()
      // Now that we have an email outbox, we need to build the personalisation twice and can't share it between two calls to sendEmail:
      staticMock.verify({ NotificationClient.prepareUpload(any(), any()) }, times(2))

      verify(notifyClient, times(1)).sendEmail(eq("partialTemplateId"), eq("sender"), any(), eq(batchId))
      verify(notifyClient, times(1)).sendEmail(eq("partialTemplateId"), eq("originalSender"), any(), eq(batchId))
    }
  }

  @Test
  fun `it should include a truncation message in partial ingestion emails when there are more errors than displayed`() {
    whenever(notifyProperties.enabled).thenReturn(true)

    val errors = (1..7).map { i ->
      EmailAttachmentIngestionError(
        rowNumber = i.toLong(),
        crimeReference = "CRI0000000$i",
        crimeTypeId = null,
        errorType = CrimeBatchEmailAttachmentIngestionErrorType.INVALID_CRIME_TYPE,
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

    val personalisation = mapOf(
      "fileName" to "attachment.csv",
      "ingestionDate" to utcToday,
      "batchId" to batchId,
      "policeForce" to PoliceForce.METROPOLITAN.name,
      "errorSummary" to """
        Row 1: Field must be a valid ENUM value (crimeTypeId)
        Row 2: Field must be a valid ENUM value (crimeTypeId)
        Row 3: Field must be a valid ENUM value (crimeTypeId)
        Row 4: Field must be a valid ENUM value (crimeTypeId)
        Row 5: Field must be a valid ENUM value (crimeTypeId)
        ...and 2 more errors
      """.trimIndent(),
      "totalCount" to 10,
      "successCount" to 0,
      "failedCount" to 10,
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
        batchId = batchId,
        policeForce = PoliceForce.METROPOLITAN.name,
        emailData = emailData,
        errors = errors,
        ingestionStatus = IngestionStatus.PARTIAL,
        recordCount = 10,
      )

      val outboxCaptor = argumentCaptor<EmailOutbox>()

      service.createEmailOutboxRequest(ingestionOutcome)
      verify(emailOutboxRepository, times(2)).save(outboxCaptor.capture())
      val claimedRows = outboxCaptor.allValues
      claimedRows.forEach { row -> row.claimedAt = Instant.now() }
      whenever(emailOutboxRepository.claimEligibleRows(eq(EmailOutboxState.PENDING.name), any(), any())).thenReturn(claimedRows)

      service.sendEmails()
      // Now that we have an email outbox, we need to build the personalisation twice and can't share it between two calls to sendEmail:
      staticMock.verify({ NotificationClient.prepareUpload(any(), any()) }, times(2))
    }

    verify(notifyClient, times(1)).sendEmail("partialTemplateId", "sender", personalisation, batchId)
    verify(notifyClient, times(1)).sendEmail("partialTemplateId", "originalSender", personalisation, batchId)
  }

  @Test
  fun `it should include a truncation message in error ingestion emails when there are more errors than displayed`() {
    whenever(notifyProperties.enabled).thenReturn(true)

    val errors = (1..7).map { i ->
      EmailAttachmentIngestionError(
        rowNumber = i.toLong(),
        crimeReference = "CRI0000000$i",
        crimeTypeId = null,
        errorType = CrimeBatchEmailAttachmentIngestionErrorType.INVALID_CRIME_TYPE,
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

    val personalisation = mapOf(
      "fileName" to "attachment.csv",
      "ingestionDate" to utcToday,
      "batchId" to batchId,
      "policeForce" to PoliceForce.METROPOLITAN.name,
      "errorSummary" to """
        Row 1: Field must be a valid ENUM value (crimeTypeId)
        Row 2: Field must be a valid ENUM value (crimeTypeId)
        Row 3: Field must be a valid ENUM value (crimeTypeId)
        Row 4: Field must be a valid ENUM value (crimeTypeId)
        Row 5: Field must be a valid ENUM value (crimeTypeId)
        ...and 2 more errors
      """.trimIndent(),
      "totalCount" to 10,
      "successCount" to 0,
      "failedCount" to 10,
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
        batchId = batchId,
        policeForce = PoliceForce.METROPOLITAN.name,
        emailData = emailData,
        errors = errors,
        ingestionStatus = IngestionStatus.ERROR,
        recordCount = 10,
      )

      val outboxCaptor = argumentCaptor<EmailOutbox>()

      service.createEmailOutboxRequest(ingestionOutcome)
      verify(emailOutboxRepository, times(2)).save(outboxCaptor.capture())
      val claimedRows = outboxCaptor.allValues
      claimedRows.forEach { row -> row.claimedAt = Instant.now() }
      whenever(emailOutboxRepository.claimEligibleRows(eq(EmailOutboxState.PENDING.name), any(), any())).thenReturn(claimedRows)

      service.sendEmails()
      // Now that we have an email outbox, we need to build the personalisation twice and can't share it between two calls to sendEmail:
      staticMock.verify({ NotificationClient.prepareUpload(any(), any()) }, times(2))
    }

    verify(notifyClient, times(1)).sendEmail("errorTemplateId", "sender", personalisation, batchId)
    verify(notifyClient, times(1)).sendEmail("errorTemplateId", "originalSender", personalisation, batchId)
  }

  @Test
  fun `it should send an error ingestion email with errorSummary and CSV attachment when notify is enabled`() {
    whenever(notifyProperties.enabled).thenReturn(true)

    val errors = listOf(
      EmailAttachmentIngestionError(
        rowNumber = 1,
        crimeReference = "CRI00000001",
        crimeTypeId = null,
        errorType = CrimeBatchEmailAttachmentIngestionErrorType.INVALID_CRIME_TYPE,
        field = "crimeTypeId",
        value = "",
      ),
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

      val outboxCaptor = argumentCaptor<EmailOutbox>()

      service.createEmailOutboxRequest(ingestionOutcome)
      verify(emailOutboxRepository, times(2)).save(outboxCaptor.capture())
      val claimedRows = outboxCaptor.allValues
      claimedRows.forEach { row -> row.claimedAt = Instant.now() }
      whenever(emailOutboxRepository.claimEligibleRows(eq(EmailOutboxState.PENDING.name), any(), any())).thenReturn(claimedRows)

      service.sendEmails()
      // Now that we have an email outbox, we need to build the personalisation twice and can't share it between two calls to sendEmail:
      staticMock.verify({ NotificationClient.prepareUpload(any(), any()) }, times(2))

      verify(notifyClient, times(1)).sendEmail(eq("errorTemplateId"), eq("sender"), any(), eq(batchId))
      verify(notifyClient, times(1)).sendEmail(eq("errorTemplateId"), eq("originalSender"), any(), eq(batchId))
    }
  }

  @Test
  fun `it should not send an email to the original sender when the send police email flag is false`() {
    whenever(notifyProperties.enabled).thenReturn(true)
    whenever(featureFlagService.policeConfirmationEmailsEnabled()).thenReturn(false)

    val emailData = EmailData(
      sender = "sender",
      originalSender = "originalSender",
      subject = "subject",
      sentAt = Date.from(Instant.now()),
      attachments = emptyList(),
    )

    val personalisation = mapOf(
      "fileName" to "Invalid File",
      "ingestionDate" to utcToday,
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

    assertDoesNotThrow {
      val outboxCaptor = argumentCaptor<EmailOutbox>()

      service.createEmailOutboxRequest(ingestionOutcome)
      verify(emailOutboxRepository, times(1)).save(outboxCaptor.capture())
      val claimedRows = outboxCaptor.allValues
      claimedRows.forEach { row -> row.claimedAt = Instant.now() }
      whenever(emailOutboxRepository.claimEligibleRows(eq(EmailOutboxState.PENDING.name), any(), any())).thenReturn(claimedRows)

      service.sendEmails()
    }

    verify(notifyClient, times(1)).sendEmail("failedTemplateId", "sender", personalisation, "Unknown due to an error")
    verify(notifyClient, times(0)).sendEmail("failedTemplateId", "originalSender", personalisation, "Unknown due to an error")
  }

  @Test
  fun `it should catch exception from objectMapper readValue and mark row as FAILED`() {
    val mockObjectMapper = mock<ObjectMapper>()
    val testException = RuntimeException("Failed to deserialize JSON")

    whenever(mockObjectMapper.readValue(any<String>(), any<Class<*>>())).thenThrow(testException)

    val serviceWithMockedMapper = EmailNotificationService(
      featureFlagService,
      notifyClient,
      notifyProperties,
      emailOutboxRepository,
      mockObjectMapper,
    )

    val claimedAt = Instant.now()
    val emailOutbox = EmailOutbox(
      payload = """{"type":"NOTIFY_EMAIL_REQUEST"}""",
      state = EmailOutboxState.PENDING,
      claimedAt = claimedAt,
    )

    whenever(emailOutboxRepository.claimEligibleRows(eq(EmailOutboxState.PENDING.name), any(), any())).thenReturn(listOf(emailOutbox))

    serviceWithMockedMapper.sendEmails()

    verify(emailOutboxRepository, times(1)).completeClaimedRow(
      id = emailOutbox.id,
      claimedAt = claimedAt,
      state = EmailOutboxState.FAILED.name,
      attempts = 1,
      lastError = "Failed to deserialize JSON",
      version = emailOutbox.version,
    )
    verify(notifyClient, times(0)).sendEmail(any(), any(), any(), any())
  }

  @Test
  fun `it should send emails from outbox even if notify is disabled to ensure all committed emails are sent`() {
    // This behaviour is important to ensure that old emails are not sent when notify is enabled after a long time.
    whenever(notifyProperties.enabled).thenReturn(false)
    whenever(notifyProperties.successfulIngestionTemplateId).thenReturn("successTemplateId")

    val attachment = ByteArrayDataSource("data", "message/rfc822")
    attachment.name = "attachment.csv"

    val emailData = EmailData(
      sender = "sender@example.com",
      originalSender = "originalSender@example.com",
      subject = "subject",
      sentAt = Date.from(Instant.now()),
      attachments = listOf(attachment),
    )

    mockStatic(NotificationClient::class.java).use { staticMock ->
      staticMock
        .`when`<Any> {
          NotificationClient.prepareUpload(
            any(),
            any(),
          )
        }
        .thenReturn(JSONObject())

      val ingestionOutcome = EmailIngestionOutcome(
        batchId = "batchId",
        policeForce = "BEDFORDSHIRE",
        emailData = emailData,
        ingestionStatus = IngestionStatus.SUCCESSFUL,
      )

      val outboxCaptor = argumentCaptor<EmailOutbox>()

      // Create the email outbox when notify is enabled
      whenever(notifyProperties.enabled).thenReturn(true)
      service.createEmailOutboxRequest(ingestionOutcome)
      verify(emailOutboxRepository, times(2)).save(outboxCaptor.capture())
      val claimedRows = outboxCaptor.allValues

      // Now disable notify, but the emails should still be sent from outbox
      whenever(notifyProperties.enabled).thenReturn(false)
      whenever(emailOutboxRepository.claimEligibleRows(eq(EmailOutboxState.PENDING.name), any(), any())).thenReturn(claimedRows)

      service.sendEmails()
    }

    // Verify that emails were still sent even though notify is now disabled
    verify(notifyClient, times(2)).sendEmail(eq("successTemplateId"), any(), any(), eq("batchId"))
  }

  @Test
  fun `it should not attempt to mark a row as FAILED when recording PUBLISHED throws after send succeeds`() {
    whenever(notifyProperties.enabled).thenReturn(true)
    whenever(featureFlagService.policeConfirmationEmailsEnabled()).thenReturn(false)

    val attachment = ByteArrayDataSource("data", "message/rfc822")
    attachment.name = "attachment.csv"

    val emailData = EmailData(
      sender = "sender@example.com",
      originalSender = "originalSender@example.com",
      subject = "subject",
      sentAt = Date.from(Instant.now()),
      attachments = listOf(attachment),
    )

    val batchId = "batchId"
    val ingestionOutcome = EmailIngestionOutcome(
      batchId = batchId,
      policeForce = "BEDFORDSHIRE",
      emailData = emailData,
      ingestionStatus = IngestionStatus.SUCCESSFUL,
    )

    val outboxCaptor = argumentCaptor<EmailOutbox>()
    service.createEmailOutboxRequest(ingestionOutcome)
    verify(emailOutboxRepository, times(1)).save(outboxCaptor.capture())

    val claimedRows = outboxCaptor.allValues
    claimedRows.forEach { row -> row.claimedAt = Instant.now() }
    whenever(emailOutboxRepository.claimEligibleRows(eq(EmailOutboxState.PENDING.name), any(), any())).thenReturn(claimedRows)

    whenever(emailOutboxRepository.completeClaimedRow(any(), any(), eq(EmailOutboxState.PUBLISHED.name), any(), isNull(), any()))
      .thenThrow(RuntimeException("Could not record success"))
    whenever(emailOutboxRepository.completeClaimedRow(any(), any(), eq(EmailOutboxState.FAILED.name), any(), any(), any()))
      .thenReturn(1)

    assertThrows<RuntimeException> {
      service.sendEmails()
    }

    verify(notifyClient, times(1)).sendEmail(eq("templateId"), any(), any(), eq(batchId))
    verify(emailOutboxRepository, times(1)).completeClaimedRow(any(), any(), eq(EmailOutboxState.PUBLISHED.name), any(), isNull(), any())
    verify(emailOutboxRepository, times(0)).completeClaimedRow(any(), any(), eq(EmailOutboxState.FAILED.name), any(), any(), any())
  }
}
