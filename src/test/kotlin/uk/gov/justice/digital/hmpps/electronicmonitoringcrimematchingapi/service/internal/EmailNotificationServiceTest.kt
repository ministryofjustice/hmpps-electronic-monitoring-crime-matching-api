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
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config.notify.NotifyProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.EmailData
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.FailedRecord
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.ParseResult
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce
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
      attachment = attachment,
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
    val attachment = ByteArrayDataSource("data", "message/rfc822")
    attachment.name = "attachment.csv"

    val emailData = EmailData(
      sender = "sender",
      originalSender = "originalSender",
      subject = "subject",
      sentAt = Date.from(Instant.now()),
      attachment = attachment,
    )

    val failedRecords = listOf(
      FailedRecord(
        rowNumber = 1,
        errorMessage = "Police Force must be one of AVON_AND_SOMERSET, BEDFORDSHIRE, CHESHIRE, CITY_OF_LONDON, CUMBRIA, DERBYSHIRE, DURHAM, ESSEX, GLOUCESTERSHIRE, GWENT, HAMPSHIRE, HERTFORDSHIRE, HUMBERSIDE, KENT, METROPOLITAN, NORTH_WALES, NOTTINGHAMSHIRE, SUSSEX, WEST_MIDLANDS but was 'invalid police force' on row 1.\n",
        originalCsvRow = "invalid,TOMV,TOMV,MPS20260126,CRI00000006,20260126083000,20260126103000,,,,54.73241,-1.38542,WGS84,\n",
      )
    )

    val parseResult = ParseResult(
      recordCount = 1,
      records = emptyList(),
      errors = emptyList(),
      failedRecords = failedRecords,
    )

    val uploadFile = JSONObject()
    val errorSummary = "Row 1: Police Force must be one of AVON_AND_SOMERSET, BEDFORDSHIRE, CHESHIRE, CITY_OF_LONDON, CUMBRIA, DERBYSHIRE, DURHAM, ESSEX, GLOUCESTERSHIRE, GWENT, HAMPSHIRE, HERTFORDSHIRE, HUMBERSIDE, KENT, METROPOLITAN, NORTH_WALES, NOTTINGHAMSHIRE, SUSSEX, WEST_MIDLANDS but was 'invalid police force' on row 1.\n"

    val personalisation = mutableMapOf(
      "fileName" to "attachment.csv",
      "totalRecords" to 1,
      "failedCount" to 1,
      "batchId" to "batchId",
      "policeForce" to "METROPOLITAN",
      "linkToFile" to uploadFile,
      "errorSummary" to errorSummary,
      "ingestionDate" to LocalDate.now().toString(),
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

      service.sendFailedIngestionEmail(
        batchId = "batchId",
        policeForce = "Unknown",
        emailData = emailData,
        parseResult = parseResult,
        errorSummary = errorSummary,
      )
    }

    verify(notifyClient, times(1)).sendEmail("failedTemplateId", "sender", personalisation, "batchId")
    verify(notifyClient, times(1)).sendEmail("failedTemplateId", "originalSender", personalisation, "batchId")
  }

  @Test
  fun `it should not send a failed ingestion email when notify is not enabled`() {
    whenever(notifyProperties.enabled).thenReturn(false)
    val attachment = ByteArrayDataSource("data", "message/rfc822")
    attachment.name = "attachment.csv"

    val emailData = EmailData(
      sender = "sender",
      originalSender = "originalSender",
      subject = "subject",
      sentAt = Date.from(Instant.now()),
      attachment = attachment,
    )

    val failedRecords = listOf(
      FailedRecord(
        rowNumber = 1,
        errorMessage = "Invalid Batch ID format row 1.",
        originalCsvRow = "Metropolitan,TOMV,TOMV,invalid,CRI00000007,20260126083000,20260126103000,,,,54.73241,-1.38542,WGS84,\n",
      )
    )

    val parseResult = ParseResult(
      recordCount = 1,
      records = emptyList(),
      errors = listOf("Invalid Batch ID row 1."),
      failedRecords = failedRecords,
    )

    val errorSummary = "Row 1: Invalid Batch ID."

    assertDoesNotThrow {
      service.sendFailedIngestionEmail(
        batchId = "batchId",
        policeForce = "Unknown",
        emailData = emailData,
        parseResult = parseResult,
        errorSummary = errorSummary,
      )
    }

    verify(notifyClient, times(0)).sendEmail(any(), any(), any(), any(), any())
  }

  @Test
  fun `it should send a partial ingestion email when notify is enabled`() {
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

    val failedRecords = listOf(
      FailedRecord(
        rowNumber = 2,
        errorMessage = "crimeType must be one of RB, BIAD, AB, BOTD, TOMV, TFP, TFMV but was 'invalid crime type' on row 1.\n",
        originalCsvRow = "Metropolitan,invalid,invalid,MPS20260126,CRI00000008,20260126083000,20260126103000,,,,54.73241,-1.38542,WGS84,\n",
      )
    )

    val parseResult = ParseResult(
      recordCount = 3,
      records = emptyList(),
      errors = listOf("crimeType must be one of RB, BIAD, AB, BOTD, TOMV, TFP, TFMV but was 'invalid crime type' on row 1.\n"),
      failedRecords = failedRecords,
    )

    val uploadFile = JSONObject()
    val errorSummary = "Row 1: crimeType must be one of RB, AB, BOTD, TOMV, TFP, TFMV but was 'invalid crime type' on row 1.\n"

    val personalisation = mutableMapOf(
      "fileName" to "attachment.csv",
      "totalRecords" to "3",
      "failedCount" to "1",
      "linkToFile" to uploadFile,
      "successCount" to "2",
      "batchId" to "batchId",
      "policeForce" to "METROPOLITAN",
      "errorSummary" to errorSummary,
      "ingestionDate" to LocalDate.now().toString(),
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

      service.sendPartialIngestionEmail(
        batchId = "batchId",
        policeForce = PoliceForce.METROPOLITAN,
        emailData = emailData,
        records = emptyList(),
        parseResult = parseResult,
        successCount = 2,
        failedCount = 1,
        totalRecords = 3,
        errorSummary = errorSummary,
      )
    }

    verify(notifyClient, times(1)).sendEmail("partialTemplateId", "sender", personalisation, "batchId")
    verify(notifyClient, times(1)).sendEmail("partialTemplateId", "originalSender", personalisation, "batchId")
  }

  @Test
  fun `it should not send a partial ingestion email when notify is not enabled`() {
    whenever(notifyProperties.enabled).thenReturn(false)
    val attachment = ByteArrayDataSource("data", "message/rfc822")
    attachment.name = "attachment.csv"

    val emailData = EmailData(
      sender = "sender",
      originalSender = "originalSender",
      subject = "subject",
      sentAt = Date.from(Instant.now()),
      attachment = attachment,
    )

    val failedRecords = listOf(
      FailedRecord(
        rowNumber = 2,
        errorMessage = "crimeType must be one of RB, BIAD, AB, BOTD, TOMV, TFP, TFMV but was 'invalid crime type' on row 1.\n",
        originalCsvRow = "Metropolitan,invalid,invalid,MPS20260126,CRI00000008,20260126083000,20260126103000,,,,54.73241,-1.38542,WGS84,\n",
      )
    )

    val parseResult = ParseResult(
      recordCount = 3,
      records = emptyList(),
      errors = listOf("crimeType must be one of RB, BIAD, AB, BOTD, TOMV, TFP, TFMV but was 'invalid crime type' on row 1.\n"),
      failedRecords = failedRecords,
    )

    val errorSummary = "Row 1: crimeType must be one of RB, AB, BOTD, TOMV, TFP, TFMV but was 'invalid crime type' on row 1.\n"

    assertDoesNotThrow {
      service.sendPartialIngestionEmail(
        batchId = "batchId",
        policeForce = PoliceForce.METROPOLITAN,
        emailData = emailData,
        records = emptyList(),
        parseResult = parseResult,
        successCount = 2,
        failedCount = 1,
        totalRecords = 3,
        errorSummary = errorSummary,
      )
    }

    verify(notifyClient, times(0)).sendEmail(any(), any(), any(), any(), any())
  }
}
