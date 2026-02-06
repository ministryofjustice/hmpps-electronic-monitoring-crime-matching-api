package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.validation.Validation
import jakarta.validation.ValidationException
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.test.context.ActiveProfiles
import software.amazon.awssdk.core.ResponseInputStream
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helper.createCsvRow
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helper.createEmailFile
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helper.createEmailFileInvalidSubject
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helper.createEmailFileWithMultipleAttachments
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helper.createEmailFileWithoutAttachment
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.SqsMessage
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatch
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmail
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmailAttachment
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchIngestionAttempt
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch.CrimeBatchCsvService
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch.CrimeBatchEmailIngestionService
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch.CrimeBatchService
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
  private lateinit var emailNotificationService: EmailNotificationService

  private val mapper: ObjectMapper = jacksonObjectMapper()
  private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

  @BeforeEach
  fun setup() {
    s3Service = Mockito.mock(S3Service::class.java)
    crimeBatchCsvService = CrimeBatchCsvService(validator)
    crimeBatchEmailIngestionService = Mockito.mock(CrimeBatchEmailIngestionService::class.java)
    crimeBatchService = Mockito.mock(CrimeBatchService::class.java)
    emailNotificationService = Mockito.mock(EmailNotificationService::class.java)
    listener = EmailListener(mapper, s3Service, crimeBatchCsvService, crimeBatchEmailIngestionService, crimeBatchService, emailNotificationService)
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

      whenever(s3Service.getObject(messageId.toString(), "email-file", "emails")).thenReturn(responseStream)
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
      verify(emailNotificationService, times(1)).sendSuccessfulIngestionEmail(any(), any(), any(), any())
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

      assertThrows<ValidationException> {
        listener.receiveEmailNotification(sqsMessage)
      }
    }

    @Test
    fun `it should throw an exception when the email file contains no attachments`() {
      val message = """
        {
          "receipt" : {
            "action" : {
              "bucketName" : "emails",
              "objectKey" : "email-file-no-attachment"
            }
          }
        }
      """.trimIndent()
      val messageId = UUID.randomUUID()
      val sqsMessage = SqsMessage("Notification", message, messageId)
      val responseStream = ResponseInputStream(
        GetObjectResponse.builder().build(),
        createEmailFileWithoutAttachment().byteInputStream(),
      )

      whenever(s3Service.getObject(messageId.toString(), "email-file-no-attachment", "emails")).thenReturn(responseStream)

      val exception = assertThrows<ValidationException> {
        listener.receiveEmailNotification(sqsMessage)
      }
      assertThat(exception.message).isEqualTo("Failed to process email: No CSV attachment found in email")
    }

    @Test
    fun `it should throw an exception when the email file contains multiple attachments`() {
      val message = """
        {
          "receipt" : {
            "action" : {
              "bucketName" : "emails",
              "objectKey" : "email-file-multiple-attachments"
            }
          }
        }
      """.trimIndent()
      val messageId = UUID.randomUUID()
      val sqsMessage = SqsMessage("Notification", message, messageId)
      val responseStream = ResponseInputStream(
        GetObjectResponse.builder().build(),
        createEmailFileWithMultipleAttachments().byteInputStream(),
      )

      whenever(s3Service.getObject(messageId.toString(), "email-file-multiple-attachments", "emails")).thenReturn(responseStream)

      val exception = assertThrows<ValidationException> {
        listener.receiveEmailNotification(sqsMessage)
      }
      assertThat(exception.message).isEqualTo("Failed to process email: Multiple CSV attachments found")
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
        createEmailFileInvalidSubject().byteInputStream(),
      )

      whenever(s3Service.getObject(messageId.toString(), "email-file-invalid-subject", "emails")).thenReturn(responseStream)

      val exception = assertThrows<ValidationException> {
        listener.receiveEmailNotification(sqsMessage)
      }
      assertThat(exception.message).isEqualTo("Failed to process email: Invalid email subject")
    }
  }
}
