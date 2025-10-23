package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.validation.ValidationException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.kotlin.whenever
import org.springframework.core.io.ClassPathResource
import org.springframework.test.context.ActiveProfiles
import software.amazon.awssdk.core.ResponseInputStream
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.SqsMessage
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch.CrimeBatchService
import java.util.UUID

@ActiveProfiles("test")
class EmailListenerTest {
  private lateinit var listener: EmailListener
  private lateinit var s3Service: S3Service
  private lateinit var crimeBatchService: CrimeBatchService

  private val mapper: ObjectMapper = jacksonObjectMapper()

  @BeforeEach
  fun setup() {
    s3Service = Mockito.mock(S3Service::class.java)
    crimeBatchService = Mockito.mock(CrimeBatchService::class.java)
    listener = EmailListener(mapper, s3Service, crimeBatchService)
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
      val fileData = ClassPathResource("emailExamples/email-file").inputStream
      val sqsMessage = SqsMessage("Notification", message, UUID.randomUUID())
      val responseStream = ResponseInputStream(
        GetObjectResponse.builder().build(),
        fileData,
      )
      whenever(s3Service.getObject("email-file", "emails")).thenReturn(responseStream)
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
      val fileData = ClassPathResource("emailExamples/email-file-no-attachment").inputStream
      val sqsMessage = SqsMessage("Notification", message, UUID.randomUUID())
      val responseStream = ResponseInputStream(
        GetObjectResponse.builder().build(),
        fileData,
      )
      whenever(s3Service.getObject("email-file-no-attachment", "emails")).thenReturn(responseStream)
      assertThrows<ValidationException> {
        listener.receiveEmailNotification(sqsMessage)
      }
    }
  }
}
