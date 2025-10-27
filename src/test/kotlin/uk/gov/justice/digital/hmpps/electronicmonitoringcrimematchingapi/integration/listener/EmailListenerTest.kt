package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.listener

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
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
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import software.amazon.awssdk.services.sqs.model.SendMessageResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeBatchRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeRepository
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue
import java.util.concurrent.CompletableFuture

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
  lateinit var hmppsQueueService: HmppsQueueService

  @MockitoSpyBean
  lateinit var crimeBatchRepository: CrimeBatchRepository

  @MockitoSpyBean
  lateinit var crimeRepository: CrimeRepository

  val emailQueueConfig by lazy {
    hmppsQueueService.findByQueueId("email")
      ?: throw MissingQueueException("HmppsQueue email not found")
  }
  val emailQueueSqsUrl by lazy { emailQueueConfig.queueUrl }
  val emailQueueSqsClient by lazy { emailQueueConfig.sqsClient }
  val emailDeadLetterSqsClient by lazy { emailQueueConfig.sqsDlqClient as SqsAsyncClient }
  val emailDeadLetterSqsUrl by lazy { emailQueueConfig.dlqUrl as String }

  @BeforeAll
  fun beforeAll() {
    s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build())
  }

  @BeforeEach
  fun beforeEach() {
    emailQueueSqsClient.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(emailQueueSqsUrl).build(),
    ).get()
    emailDeadLetterSqsClient.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(emailDeadLetterSqsUrl).build(),
    ).get()
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
          "Type" : "Notification",
          "MessageId" : "4730435b-88b9-5b6c-a91c-9b1236b456f7",
          "TopicArn" : "arn:aws:sns:eu-west-2:000000000000:email-topic",
          "Message" : "{ \"notificationType\": \"Received\", \"receipt\": { \"action\": { \"bucketName\": \"$BUCKET_NAME\", \"objectKey\": \"$OBJECT_KEY\" }}}"
        }
      """.trimIndent()
      val fileData = ClassPathResource("emailExamples/$OBJECT_KEY")

      s3Client.putObject(PutObjectRequest.builder().bucket(BUCKET_NAME).key(OBJECT_KEY).build(), RequestBody.fromFile(fileData.file))

      sendDomainSqsMessage(message)

      await().until { getNumberOfMessagesCurrentlyOnQueue() == 0 }

      val crimeBatches = crimeBatchRepository.findAll()
      assertThat(crimeBatches).hasSize(1)
      assertThat(crimeBatches.first()).isNotNull()
      val crimes = crimeRepository.findAll()
      assertThat(crimes).isNotEmpty()
      assertThat(crimes).hasSize(3)
    }

    @Test
    fun `it should move the message to the dead letter queue when the message content is missing s3 object details`() {
      val message = """
        {
          "Type" : "Notification",
          "MessageId" : "4730435b-88b9-5b6c-a91c-9b1236b456f7",
          "TopicArn" : "arn:aws:sns:eu-west-2:000000000000:email-topic",
          "Message" : "{ \"notificationType\": \"Received\", \"receipt\": { \"action\": { \"bucketName\": \"$BUCKET_NAME\" }}}"
        }
      """.trimIndent()

      sendDomainSqsMessage(message)

      await().until { getNumberOfMessagesCurrentlyOnDeadLetterQueue() == 1 }

      val dlqMessage = getMessagesCurrentlyOnDeadLetterQueue().messages().first()
      assertThat(dlqMessage.body()).isEqualTo(message)
    }

    fun sendDomainSqsMessage(rawMessage: String): CompletableFuture<SendMessageResponse> = emailQueueSqsClient.sendMessage(
      SendMessageRequest.builder().queueUrl(
        emailQueueSqsUrl,
      ).messageBody(rawMessage).build(),
    )

    fun getNumberOfMessagesCurrentlyOnQueue(): Int = emailQueueSqsClient.countAllMessagesOnQueue(
      emailQueueSqsUrl,
    ).get()

    fun getNumberOfMessagesCurrentlyOnDeadLetterQueue(): Int = emailDeadLetterSqsClient.countAllMessagesOnQueue(
      emailDeadLetterSqsUrl,
    ).get()

    fun getMessagesCurrentlyOnDeadLetterQueue(): ReceiveMessageResponse = emailDeadLetterSqsClient.receiveMessage(
      ReceiveMessageRequest.builder().queueUrl(emailDeadLetterSqsUrl).build(),
    ).get()
  }
}
