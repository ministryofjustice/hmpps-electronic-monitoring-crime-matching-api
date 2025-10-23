package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.listener

import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import software.amazon.awssdk.core.ResponseInputStream
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.SqsMessage
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeBatchRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal.EmailListener
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal.S3Service
import java.util.UUID

@ActiveProfiles("integration")
class EmailListenerTest : IntegrationTestBase() {

  @MockitoBean
  lateinit var s3Service: S3Service

  @Autowired
  lateinit var emailListener: EmailListener

  @MockitoSpyBean
  lateinit var crimeBatchRepository: CrimeBatchRepository

  @MockitoSpyBean
  lateinit var crimeRepository: CrimeRepository

  @Nested
  @DisplayName("receiveEmailNotification")
  inner class ReceiveEmailNotification {
    @Test
    fun `it should process a valid email notification`() {
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

      emailListener.receiveEmailNotification(sqsMessage)
      val crimeBatch = crimeBatchRepository.findAll().firstOrNull()
      assertThat(crimeBatch).isNotNull()
      val crimes = crimeRepository.findAll()
      assertThat(crimes).isNotEmpty()
      assertThat(crimes).hasSize(3)
    }

    @Test
    fun `it should throw a ValidationException when the message content is missing s3 details`() {
      val message = """
        {
          "receipt" : {
            "action" : {
              "bucketName" : "emails"
            }
          }
        }
      """.trimIndent()
      val sqsMessage = SqsMessage("Notification", message, UUID.randomUUID())

      assertThrows<ValidationException> {
        emailListener.receiveEmailNotification(sqsMessage)
      }
    }
  }
}
