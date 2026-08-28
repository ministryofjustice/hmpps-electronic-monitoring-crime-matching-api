package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinInvalidNullException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.SimpleTransactionStatus
import software.amazon.awssdk.core.ResponseInputStream
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config.emailIngestion.EmailIngestionProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helper.createCsvRow
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helper.createEmailFile
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helper.createEmailFileNoFromAddress
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helper.createEmailFileNoRedirect
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.SqsMessage
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatch
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmail
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmailAttachment
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchIngestionAttempt
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.MatchingPublishState
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.MatchingNotificationService
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch.CrimeBatchCsvService
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch.CrimeBatchEmailIngestionService
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch.CrimeBatchService
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.outbox.EmailOutboxService
import java.time.Instant
import java.util.Date
import java.util.UUID
import kotlin.io.encoding.Base64

@ActiveProfiles("test")
class EmailListenerTest {
  private lateinit var listener: EmailListener
  private lateinit var s3Service: S3Service
  private lateinit var crimeBatchCsvService: CrimeBatchCsvService
  private lateinit var crimeBatchEmailIngestionService: CrimeBatchEmailIngestionService
  private lateinit var crimeBatchService: CrimeBatchService
  private lateinit var emailOutboxService: EmailOutboxService
  private lateinit var emailParserService: EmailParserService
  private lateinit var matchingNotificationService: MatchingNotificationService
  private lateinit var metricsService: MetricsService
  private lateinit var transactionManager: PlatformTransactionManager

  private val mapper: ObjectMapper = jacksonObjectMapper()
  private val emailIngestionProperties: EmailIngestionProperties = EmailIngestionProperties(
    mailboxAddress = "shared-mailbox@email.com",
  )

  @BeforeEach
  fun setup() {
    s3Service = Mockito.mock(S3Service::class.java)
    crimeBatchCsvService = CrimeBatchCsvService()
    crimeBatchEmailIngestionService = Mockito.mock(CrimeBatchEmailIngestionService::class.java)
    crimeBatchService = Mockito.mock(CrimeBatchService::class.java)
    emailOutboxService = Mockito.mock(EmailOutboxService::class.java)
    emailParserService = EmailParserService(emailIngestionProperties)
    matchingNotificationService = Mockito.mock(MatchingNotificationService::class.java)
    metricsService = Mockito.mock(MetricsService::class.java)
    transactionManager = Mockito.mock(PlatformTransactionManager::class.java)
    whenever(transactionManager.getTransaction(any())).thenReturn(SimpleTransactionStatus())
    listener = EmailListener(mapper, s3Service, crimeBatchCsvService, crimeBatchEmailIngestionService, crimeBatchService, emailOutboxService, emailParserService, matchingNotificationService, metricsService, transactionManager)
  }

  @Nested
  @DisplayName("receiveEmailNotification")
  inner class ReceiveEmailNotification {
    @Test
    fun `it should successfully receive and process an email notification`() {
      val message = """
        {
          "receipt" : {
            "action" : {
              "bucketName" : "emails",
              "objectKey" : "email-file"
            }
          }
        }
      """.trimIndent()
      val messageId = UUID.randomUUID()
      val sqsMessage = SqsMessage("Notification", message, messageId)

      val csvContent = listOf(
        createCsvRow(),
      ).joinToString("\n")
      val encoded = Base64.encode(csvContent.toByteArray())

      val responseStream = ResponseInputStream(
        GetObjectResponse.builder().build(),
        createEmailFile(encoded).byteInputStream(),
      )

      val crimeBatchIngestionAttempt = CrimeBatchIngestionAttempt(
        bucket = "emails",
        objectName = "email-file",
      )

      whenever(s3Service.getObject(messageId, "email-file", "emails")).thenReturn(responseStream)
      whenever(crimeBatchEmailIngestionService.createCrimeBatchIngestionAttempt("emails", "email-file")).thenReturn(
        crimeBatchIngestionAttempt,
      )

      val crimeBatchEmail = CrimeBatchEmail(
        crimeBatchIngestionAttempt = crimeBatchIngestionAttempt,
        sender = "sender",
        originalSender = "originalSender",
        subject = "subject",
        sentAt = Date.from(Instant.now()),
      )

      val crimeBatchEmailAttachment = CrimeBatchEmailAttachment(
        crimeBatchEmail = crimeBatchEmail,
        fileName = "filename",
        rowCount = 1,
      )

      val crimeBatch = CrimeBatch(
        batchId = "batchId",
        crimeBatchEmailAttachment = crimeBatchEmailAttachment,
      )

      whenever(crimeBatchEmailIngestionService.createCrimeBatchEmailAttachment(any(), any(), any())).thenReturn(
        crimeBatchEmailAttachment,
      )

      whenever(crimeBatchService.createCrimeBatch(any(), any())).thenReturn(
        crimeBatch,
      )

      whenever(crimeBatchEmailIngestionService.createCrimeBatchEmail(any(), any())).thenReturn(
        CrimeBatchEmail(
          crimeBatchIngestionAttempt = crimeBatchIngestionAttempt,
          sender = "sender",
          originalSender = "originalSender",
          subject = "subject",
          sentAt = Date.from(Instant.now()),
        ),
      )

      assertDoesNotThrow { listener.receiveEmailNotification(sqsMessage) }

      val notificationCaptor = argumentCaptor<String>()

      val inOrder = inOrder(
        crimeBatchService,
        matchingNotificationService,
        emailOutboxService,
        metricsService,
      )

      inOrder.verify(crimeBatchService, times(1)).createCrimeBatch(any(), any())
      inOrder.verify(emailOutboxService, times(1)).enqueue(any())
      inOrder.verify(metricsService, times(1)).recordOutcome(any())
      inOrder.verify(matchingNotificationService, times(1)).publishMatchingRequest(notificationCaptor.capture())

      assertThat(notificationCaptor.allValues.first()).isEqualTo(crimeBatch.id.toString())
    }

    @Test
    fun `it should successfully receive and process an email notification with a plain subject line`() {
      val message = """
        {
          "receipt" : {
            "action" : {
              "bucketName" : "emails",
              "objectKey" : "email-file-plain-subject"
            }
          }
        }
      """.trimIndent()
      val messageId = UUID.randomUUID()
      val sqsMessage = SqsMessage("Notification", message, messageId)

      val csvContent = listOf(
        createCsvRow(),
      ).joinToString("\n")
      val encoded = Base64.encode(csvContent.toByteArray())

      val responseStream = ResponseInputStream(
        GetObjectResponse.builder().build(),
        createEmailFile(csvContent = encoded, subject = "Crime Mapping Request").byteInputStream(),
      )

      val crimeBatchIngestionAttempt = CrimeBatchIngestionAttempt(
        bucket = "emails",
        objectName = "email-file-plain-subject",
      )

      whenever(s3Service.getObject(messageId, "email-file-plain-subject", "emails")).thenReturn(responseStream)
      whenever(crimeBatchEmailIngestionService.createCrimeBatchIngestionAttempt("emails", "email-file-plain-subject")).thenReturn(
        crimeBatchIngestionAttempt,
      )

      val crimeBatchEmail = CrimeBatchEmail(
        crimeBatchIngestionAttempt = crimeBatchIngestionAttempt,
        sender = "sender",
        originalSender = "originalSender",
        subject = "subject",
        sentAt = Date.from(Instant.now()),
      )

      val crimeBatchEmailAttachment = CrimeBatchEmailAttachment(
        crimeBatchEmail = crimeBatchEmail,
        fileName = "filename",
        rowCount = 1,
      )

      val crimeBatch = CrimeBatch(
        batchId = "batchId",
        crimeBatchEmailAttachment = crimeBatchEmailAttachment,
      )

      whenever(crimeBatchEmailIngestionService.createCrimeBatchEmailAttachment(any(), any(), any())).thenReturn(
        crimeBatchEmailAttachment,
      )

      whenever(crimeBatchService.createCrimeBatch(any(), any())).thenReturn(
        crimeBatch,
      )

      whenever(crimeBatchEmailIngestionService.createCrimeBatchEmail(any(), any())).thenReturn(
        CrimeBatchEmail(
          crimeBatchIngestionAttempt = crimeBatchIngestionAttempt,
          sender = "sender",
          originalSender = "originalSender",
          subject = "subject",
          sentAt = Date.from(Instant.now()),
        ),
      )

      assertDoesNotThrow { listener.receiveEmailNotification(sqsMessage) }

      val notificationCaptor = argumentCaptor<String>()
      verify(matchingNotificationService, times(1)).publishMatchingRequest(notificationCaptor.capture())
      verify(emailOutboxService, times(1)).enqueue(any())
      verify(metricsService, times(1)).recordOutcome(any())

      assertThat(notificationCaptor.allValues.first()).isEqualTo(crimeBatch.id.toString())
    }

    @Test
    fun `it should throw an exception when the message content does not contain s3 details`() {
      val message = """
        {
          "receipt" : {
            "action" : {
              "objectKey" : "email-file"
            }
          }
        }
      """.trimIndent()
      val sqsMessage = SqsMessage("Notification", message, UUID.randomUUID())

      assertThrows<KotlinInvalidNullException> {
        listener.receiveEmailNotification(sqsMessage)
      }
    }

    @Test
    fun `it should throw an exception when the email file contains has an invalid subject`() {
      val message = """
        {
          "receipt" : {
            "action" : {
              "bucketName" : "emails",
              "objectKey" : "email-file-invalid-subject"
            }
          }
        }
      """.trimIndent()
      val messageId = UUID.randomUUID()
      val sqsMessage = SqsMessage("Notification", message, messageId)
      val responseStream = ResponseInputStream(
        GetObjectResponse.builder().build(),
        createEmailFile(subject = "invalid").byteInputStream(),
      )

      whenever(s3Service.getObject(messageId, "email-file-invalid-subject", "emails")).thenReturn(responseStream)

      val exception = assertThrows<ValidationException> {
        listener.receiveEmailNotification(sqsMessage)
      }
      assertThat(exception.message).isEqualTo("Invalid email subject")
    }

    @Test
    fun `it should throw an exception when the email file has an invalid from address`() {
      val message = """
        {
          "receipt" : {
            "action" : {
              "bucketName" : "emails",
              "objectKey" : "email-file-invalid-from"
            }
          }
        }
      """.trimIndent()
      val messageId = UUID.randomUUID()
      val sqsMessage = SqsMessage("Notification", message, messageId)
      val responseStream = ResponseInputStream(
        GetObjectResponse.builder().build(),
        createEmailFileNoFromAddress().byteInputStream(),
      )

      whenever(s3Service.getObject(messageId, "email-file-invalid-from", "emails")).thenReturn(responseStream)

      val exception = assertThrows<ValidationException> {
        listener.receiveEmailNotification(sqsMessage)
      }
      assertThat(exception.message).isEqualTo("Invalid sender email")
    }

    @Test
    fun `it should throw an exception when the email file has an invalid redirect address`() {
      val message = """
        {
          "receipt" : {
            "action" : {
              "bucketName" : "emails",
              "objectKey" : "email-file-invalid-redirect"
            }
          }
        }
      """.trimIndent()
      val messageId = UUID.randomUUID()
      val sqsMessage = SqsMessage("Notification", message, messageId)
      val responseStream = ResponseInputStream(
        GetObjectResponse.builder().build(),
        createEmailFile(resentFrom = "invalid").byteInputStream(),
      )

      whenever(s3Service.getObject(messageId, "email-file-invalid-redirect", "emails")).thenReturn(responseStream)

      val exception = assertThrows<ValidationException> {
        listener.receiveEmailNotification(sqsMessage)
      }
      assertThat(exception.message).isEqualTo("Invalid redirect email")
    }

    @Test
    fun `it should throw an exception when the email file does not have a redirect address`() {
      val message = """
        {
          "receipt" : {
            "action" : {
              "bucketName" : "emails",
              "objectKey" : "email-file-no-redirect"
            }
          }
        }
      """.trimIndent()
      val messageId = UUID.randomUUID()
      val sqsMessage = SqsMessage("Notification", message, messageId)
      val responseStream = ResponseInputStream(
        GetObjectResponse.builder().build(),
        createEmailFileNoRedirect().byteInputStream(),
      )

      whenever(s3Service.getObject(messageId, "email-file-no-redirect", "emails")).thenReturn(responseStream)

      val exception = assertThrows<ValidationException> {
        listener.receiveEmailNotification(sqsMessage)
      }
      assertThat(exception.message).isEqualTo("No redirect email")
    }

    // --- Duplicate / idempotency tests ---

    @Test
    fun `it should not ingest or enqueue when the same s3 object has already been ingested with PUBLISHED state`() {
      val existingAttempt = CrimeBatchIngestionAttempt(
        bucket = "emails",
        objectName = "email-file",
        matchingPublishState = MatchingPublishState.PUBLISHED,
        crimeBatchId = UUID.randomUUID(),
      )
      whenever(crimeBatchEmailIngestionService.findIngestionAttemptBySource("emails", "email-file"))
        .thenReturn(existingAttempt)

      val message = buildMessage("emails", "email-file")
      assertDoesNotThrow { listener.receiveEmailNotification(SqsMessage("Notification", message, UUID.randomUUID())) }

      verify(s3Service, never()).getObject(any(), any(), any())
      verify(emailOutboxService, never()).enqueue(any())
      verify(matchingNotificationService, never()).publishMatchingRequest(any())
      verify(metricsService, never()).recordOutcome(any())
    }

    @Test
    fun `it should not ingest or publish when the same s3 object has already been ingested with NOT_REQUIRED state`() {
      val existingAttempt = CrimeBatchIngestionAttempt(
        bucket = "emails",
        objectName = "email-file",
        matchingPublishState = MatchingPublishState.NOT_REQUIRED,
        crimeBatchId = null,
      )
      whenever(crimeBatchEmailIngestionService.findIngestionAttemptBySource("emails", "email-file"))
        .thenReturn(existingAttempt)

      val message = buildMessage("emails", "email-file")
      assertDoesNotThrow { listener.receiveEmailNotification(SqsMessage("Notification", message, UUID.randomUUID())) }

      verify(s3Service, never()).getObject(any(), any(), any())
      verify(matchingNotificationService, never()).publishMatchingRequest(any())
    }

    @Test
    fun `it should retry publishMatchingRequest on duplicate when prior state is PENDING_OR_UNCONFIRMED`() {
      val crimeBatchId = UUID.randomUUID()
      val existingAttempt = CrimeBatchIngestionAttempt(
        bucket = "emails",
        objectName = "email-file",
        matchingPublishState = MatchingPublishState.PENDING_OR_UNCONFIRMED,
        crimeBatchId = crimeBatchId,
      )
      whenever(crimeBatchEmailIngestionService.findIngestionAttemptBySource("emails", "email-file"))
        .thenReturn(existingAttempt)

      val message = buildMessage("emails", "email-file")
      assertDoesNotThrow { listener.receiveEmailNotification(SqsMessage("Notification", message, UUID.randomUUID())) }

      verify(s3Service, never()).getObject(any(), any(), any())
      verify(emailOutboxService, never()).enqueue(any())
      verify(matchingNotificationService, times(1)).publishMatchingRequest(crimeBatchId.toString())
      verify(crimeBatchEmailIngestionService, times(1))
        .markMatchingPublishState(existingAttempt.id, MatchingPublishState.PUBLISHED)
    }

    @Test
    fun `it should not publish on duplicate when prior state is PENDING_OR_UNCONFIRMED but crimeBatchId is null`() {
      // No crimeBatchId means there is nothing to re-publish on duplicate delivery.
      val existingAttempt = CrimeBatchIngestionAttempt(
        bucket = "emails",
        objectName = "email-file",
        matchingPublishState = MatchingPublishState.PENDING_OR_UNCONFIRMED,
        crimeBatchId = null,
      )
      whenever(crimeBatchEmailIngestionService.findIngestionAttemptBySource("emails", "email-file"))
        .thenReturn(existingAttempt)

      val message = buildMessage("emails", "email-file")
      assertDoesNotThrow { listener.receiveEmailNotification(SqsMessage("Notification", message, UUID.randomUUID())) }

      verify(matchingNotificationService, never()).publishMatchingRequest(any())
    }

    private fun buildMessage(bucketName: String, objectKey: String): String = """
      {
        "receipt" : {
          "action" : {
            "bucketName" : "$bucketName",
            "objectKey" : "$objectKey"
          }
        }
      }
    """.trimIndent()
  }
}
