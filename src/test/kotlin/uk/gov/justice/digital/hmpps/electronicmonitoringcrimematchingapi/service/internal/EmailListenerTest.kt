package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.validation.Validation
import jakarta.validation.ValidationException
import jakarta.validation.Validator
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.test.context.ActiveProfiles
import software.amazon.awssdk.core.ResponseInputStream
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helper.createEmailFile
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helper.createEmailFileWithoutAttachment
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.SqsMessage
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmail
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchIngestionAttempt
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch.CrimeBatchCsvService
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch.CrimeBatchEmailIngestionService
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch.CrimeBatchService
import java.time.Instant
import java.util.Date
import java.util.UUID

@ActiveProfiles("test")
class EmailListenerTest {
  private lateinit var listener: EmailListener
  private lateinit var s3Service: S3Service
  private lateinit var crimeBatchCsvService: CrimeBatchCsvService
  private lateinit var crimeBatchEmailIngestionService: CrimeBatchEmailIngestionService
  private lateinit var crimeBatchService: CrimeBatchService

  private val mapper: ObjectMapper = jacksonObjectMapper()
  private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

  @BeforeEach
  fun setup() {
    s3Service = Mockito.mock(S3Service::class.java)
    crimeBatchCsvService = CrimeBatchCsvService(validator)
    crimeBatchEmailIngestionService = Mockito.mock(CrimeBatchEmailIngestionService::class.java)
    crimeBatchService = Mockito.mock(CrimeBatchService::class.java)
    listener = EmailListener(mapper, s3Service, crimeBatchCsvService, crimeBatchEmailIngestionService, crimeBatchService)
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
      val responseStream = ResponseInputStream(
        GetObjectResponse.builder().build(),
        createEmailFile("").byteInputStream(),
      )

      val crimeBatchIngestionAttempt = CrimeBatchIngestionAttempt(
        bucket = "emails",
        objectName = "email-file",
      )

      whenever(s3Service.getObject(messageId.toString(), "email-file", "emails")).thenReturn(responseStream)
      whenever(crimeBatchEmailIngestionService.createCrimeBatchIngestionAttempt("emails", "email-file")).thenReturn(
        crimeBatchIngestionAttempt,
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
      val sqsMessage = SqsMessage("Notification", message, UUID.randomUUID())
      val responseStream = ResponseInputStream(
        GetObjectResponse.builder().build(),
        createEmailFileWithoutAttachment().byteInputStream(),
      )

      whenever(s3Service.getObject("messageId", "email-file-no-attachment", "emails")).thenReturn(responseStream)

      assertThrows<ValidationException> {
        listener.receiveEmailNotification(sqsMessage)
      }
    }
  }
}
