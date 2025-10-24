package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.listener

import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.ListObjectsRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.SqsMessage
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeBatchRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal.EmailListener
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("integration")
class EmailListenerTest : IntegrationTestBase() {

  companion object {
    const val BUCKET_NAME = "emails"
    const val OBJECT_KEY = "email-file"
  }

  @Autowired
  lateinit var s3Client: S3Client

  @Autowired
  lateinit var emailListener: EmailListener

  @MockitoSpyBean
  lateinit var crimeBatchRepository: CrimeBatchRepository

  @MockitoSpyBean
  lateinit var crimeRepository: CrimeRepository

  @BeforeAll
  fun beforeAll() {
    s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build())
  }

  @AfterAll
  fun afterAll() {
    val objectListing = s3Client.listObjects(ListObjectsRequest.builder().bucket(BUCKET_NAME).build())
    objectListing.contents()?.forEach {
      s3Client.deleteObject(
        DeleteObjectRequest
          .builder()
          .bucket(BUCKET_NAME)
          .key(it.key())
          .build(),
      )
    }
    s3Client.deleteBucket(DeleteBucketRequest.builder().bucket(BUCKET_NAME).build())
  }

  @Nested
  @DisplayName("receiveEmailNotification")
  inner class ReceiveEmailNotification {
    @Test
    fun `it should process a valid email notification`() {
      val message = """
        {
          "receipt" : {
            "action" : {
              "bucketName" : "$BUCKET_NAME",
              "objectKey" : "$OBJECT_KEY"
            }
          }
        }
      """.trimIndent()
      val fileData = ClassPathResource("emailExamples/$OBJECT_KEY")
      val sqsMessage = SqsMessage("Notification", message, UUID.randomUUID())

      s3Client.putObject(PutObjectRequest.builder().bucket(BUCKET_NAME).key(OBJECT_KEY).build(), RequestBody.fromFile(fileData.file))

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
              "bucketName" : "$BUCKET_NAME"
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
