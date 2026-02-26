package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.listener

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import software.amazon.awssdk.services.sqs.model.SendMessageResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helper.createCsvRow
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helper.createInvalidCsvRow
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helper.createEmailFile
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.FailedRecord
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.Crime
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeVersion
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeBatchIngestionAttemptRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeBatchRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeVersionRepository
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.CompletableFuture
import kotlin.io.encoding.Base64

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
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

  @Autowired
  lateinit var jdbcTemplate: JdbcTemplate

  @MockitoSpyBean
  lateinit var crimeBatchRepository: CrimeBatchRepository

  @MockitoSpyBean
  lateinit var crimeRepository: CrimeRepository

  @MockitoSpyBean
  lateinit var crimeVersionRepository: CrimeVersionRepository

  @MockitoSpyBean
  lateinit var crimeBatchIngestionAttemptRepository: CrimeBatchIngestionAttemptRepository

  val emailQueueConfig by lazy {
    hmppsQueueService.findByQueueId("email")
      ?: throw MissingQueueException("HmppsQueue email not found")
  }
  val emailQueueSqsUrl by lazy { emailQueueConfig.queueUrl }
  val emailQueueSqsClient by lazy { emailQueueConfig.sqsClient }
  val emailDeadLetterSqsClient by lazy { emailQueueConfig.sqsDlqClient as SqsAsyncClient }
  val emailDeadLetterSqsUrl by lazy { emailQueueConfig.dlqUrl as String }

  val matchingNotificationsQueueConfig by lazy {
    hmppsQueueService.findByQueueId("matchingnotifications")
      ?: throw MissingQueueException("HmppsQueue matchingnotifications not found")
  }
  val matchingNotificationsSqsUrl by lazy { matchingNotificationsQueueConfig.queueUrl }
  val matchingNotificationsSqsClient by lazy { matchingNotificationsQueueConfig.sqsClient }

  @BeforeEach
  fun beforeEach() {
    s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build())
    emailQueueSqsClient.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(emailQueueSqsUrl).build(),
    ).get()
    emailDeadLetterSqsClient.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(emailDeadLetterSqsUrl).build(),
    ).get()
    matchingNotificationsSqsClient.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(matchingNotificationsSqsUrl).build(),
    )
    jdbcTemplate.update("DELETE FROM crime_batch_crime_version")
    crimeBatchRepository.deleteAll()
    crimeRepository.deleteAll()
    crimeBatchIngestionAttemptRepository.deleteAll()
  }

  @AfterEach
  fun afterEach() {
    s3Client.deleteObject(
      DeleteObjectRequest
        .builder()
        .bucket(BUCKET_NAME)
        .key(OBJECT_KEY)
        .build(),
    )
    s3Client.deleteBucket(DeleteBucketRequest.builder().bucket(BUCKET_NAME).build())
  }

  @Nested
  @DisplayName("receiveEmailNotification")
  inner class ReceiveEmailNotification {
    @Test
    fun `it should process a valid email notification`() {
      val csvContent = listOf(
        createCsvRow(),
        createCsvRow(crimeReference = "CRI00000002"),
      ).joinToString("\n")

      val encoded = Base64.encode(csvContent.toByteArray())
      val email = createEmailFile(encoded)

      s3Client.putObject(PutObjectRequest.builder().bucket(BUCKET_NAME).key(OBJECT_KEY).build(), RequestBody.fromString(email))

      sendDomainSqsMessage(getMessage(OBJECT_KEY))

      await().until { getNumberOfMessagesCurrentlyOnQueue() == 0 }

      val crimeBatchIngestionAttempts = crimeBatchIngestionAttemptRepository.findAll()
      assertThat(crimeBatchIngestionAttempts).hasSize(1)
      assertThat(crimeBatchIngestionAttempts.first().crimeBatchEmail).isNotNull()
      assertThat(crimeBatchIngestionAttempts.first().crimeBatchEmail?.crimeBatchEmailAttachments).hasSize(1)

      val crimeBatches = crimeBatchRepository.findAll()
      assertThat(crimeBatches).hasSize(1)

      val crimes = crimeRepository.findAll()
      assertThat(crimes).hasSize(2)

      val crimeVersions = crimeVersionRepository.findAll()
      assertThat(crimeVersions).hasSize(2)

      // Check that notification to start algo was generated
      assertThat(getNumberOfMessagesCurrentlyOnMatchingNotificationsQueue()).isEqualTo(1)
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

      // Check that notification to start algo was not generated
      assertThat(getNumberOfMessagesCurrentlyOnMatchingNotificationsQueue()).isEqualTo(0)
    }

    @Test
    fun `it should move the message to the dead letter queue when the email contains invalid batch data`() {
      val message = getMessage(OBJECT_KEY)

      val encoded = Base64.encode(createCsvRow(policeForce = "invalid").toByteArray())
      val email = createEmailFile(encoded)

      s3Client.putObject(PutObjectRequest.builder().bucket(BUCKET_NAME).key(OBJECT_KEY).build(), RequestBody.fromString(email))

      sendDomainSqsMessage(message)

      await().until { getNumberOfMessagesCurrentlyOnDeadLetterQueue() == 1 }

      val dlqMessage = getMessagesCurrentlyOnDeadLetterQueue().messages().first()
      assertThat(dlqMessage.body()).isEqualTo(message)

      // Check that notification to start algo was not generated
      assertThat(getNumberOfMessagesCurrentlyOnMatchingNotificationsQueue()).isEqualTo(0)
    }

    @Test
    fun `it should process an email with valid and invalid crime data`() {
      val csvContent = listOf(
        createCsvRow(),
        createCsvRow(crimeTypeId = "invalid"),
        createCsvRow(crimeReference = "CRI00000002"),
      ).joinToString("\n")

      val encoded = Base64.encode(csvContent.toByteArray())
      val email = createEmailFile(encoded)

      s3Client.putObject(PutObjectRequest.builder().bucket(BUCKET_NAME).key(OBJECT_KEY).build(), RequestBody.fromString(email))

      sendDomainSqsMessage(getMessage(OBJECT_KEY))

      await().until { getNumberOfMessagesCurrentlyOnQueue() == 0 }

      val crimeBatchIngestionAttempts = crimeBatchIngestionAttemptRepository.findAll()
      assertThat(crimeBatchIngestionAttempts).hasSize(1)
      assertThat(crimeBatchIngestionAttempts.first().crimeBatchEmail).isNotNull()
      assertThat(crimeBatchIngestionAttempts.first().crimeBatchEmail?.crimeBatchEmailAttachments).hasSize(1)

      val crimeBatches = crimeBatchRepository.findAll()
      assertThat(crimeBatches).hasSize(1)

      val crimes = crimeRepository.findAll()
      assertThat(crimes).hasSize(2)

      val crimeVersions = crimeVersionRepository.findAll()
      assertThat(crimeVersions).hasSize(2)

      // Check that notification to start algo was generated
      assertThat(getNumberOfMessagesCurrentlyOnMatchingNotificationsQueue()).isEqualTo(1)
    }

    @Test
    fun `it should process an email with new crime versions`() {
      val crime = Crime(
        policeForceArea = PoliceForce.METROPOLITAN,
        crimeReference = "CRI00000001",
      )
      crimeRepository.save(crime)

      val crimeVersion = CrimeVersion(
        id = UUID.fromString("152a9a57-337f-4208-908b-2874b75fa10e"),
        crime = crime,
        crimeTypeId = CrimeType.AB,
        crimeDateTimeFrom = LocalDateTime.of(2025, 1, 25, 8, 30),
        crimeDateTimeTo = LocalDateTime.of(2025, 1, 25, 8, 30),
        easting = null,
        northing = null,
        latitude = 51.574865,
        longitude = 0.060977,
        crimeText = "",
      )
      crimeVersionRepository.save(crimeVersion)

      val csvContent = listOf(
        createCsvRow(crimeDateTimeTo = "20250125093000"),
      ).joinToString("\n")

      val encoded = Base64.encode(csvContent.toByteArray())
      val email = createEmailFile(encoded)

      s3Client.putObject(PutObjectRequest.builder().bucket(BUCKET_NAME).key(OBJECT_KEY).build(), RequestBody.fromString(email))

      sendDomainSqsMessage(getMessage(OBJECT_KEY))

      await().until { getNumberOfMessagesCurrentlyOnQueue() == 0 }

      val crimeBatchIngestionAttempts = crimeBatchIngestionAttemptRepository.findAll()
      assertThat(crimeBatchIngestionAttempts).hasSize(1)
      assertThat(crimeBatchIngestionAttempts.first().crimeBatchEmail).isNotNull()
      assertThat(crimeBatchIngestionAttempts.first().crimeBatchEmail?.crimeBatchEmailAttachments).hasSize(1)

      val crimeBatches = crimeBatchRepository.findAll()
      assertThat(crimeBatches).hasSize(1)

      val crimes = crimeRepository.findAll()
      assertThat(crimes).hasSize(1)

      val crimeVersions = crimeVersionRepository.findAll()
      assertThat(crimeVersions).hasSize(2)

      // Check that notification to start algo was generated
      assertThat(getNumberOfMessagesCurrentlyOnMatchingNotificationsQueue()).isEqualTo(1)
    }

    @Test
    fun `it should process an email with a duplicate crime version`() {
      val crime = Crime(
        policeForceArea = PoliceForce.METROPOLITAN,
        crimeReference = "CRI00000001",
      )
      crimeRepository.save(crime)

      val crimeVersion = CrimeVersion(
        id = UUID.fromString("152a9a57-337f-4208-908b-2874b75fa10e"),
        crime = crime,
        crimeTypeId = CrimeType.TOMV,
        crimeDateTimeFrom = LocalDateTime.of(2025, 1, 25, 8, 30),
        crimeDateTimeTo = LocalDateTime.of(2025, 1, 25, 8, 30),
        easting = null,
        northing = null,
        latitude = 54.73241,
        longitude = -1.38542,
        crimeText = "",
      )
      crimeVersionRepository.save(crimeVersion)

      val csvContent = listOf(
        createCsvRow(),
      ).joinToString("\n")

      val encoded = Base64.encode(csvContent.toByteArray())
      val email = createEmailFile(encoded)

      s3Client.putObject(PutObjectRequest.builder().bucket(BUCKET_NAME).key(OBJECT_KEY).build(), RequestBody.fromString(email))

      sendDomainSqsMessage(getMessage(OBJECT_KEY))

      await().until { getNumberOfMessagesCurrentlyOnQueue() == 0 }

      val crimeBatchIngestionAttempts = crimeBatchIngestionAttemptRepository.findAll()
      assertThat(crimeBatchIngestionAttempts).hasSize(1)
      assertThat(crimeBatchIngestionAttempts.first().crimeBatchEmail).isNotNull()
      assertThat(crimeBatchIngestionAttempts.first().crimeBatchEmail?.crimeBatchEmailAttachments).hasSize(1)

      val crimeBatches = crimeBatchRepository.findAll()
      assertThat(crimeBatches).hasSize(1)

      val crimes = crimeRepository.findAll()
      assertThat(crimes).hasSize(1)

      val crimeVersions = crimeVersionRepository.findAll()
      assertThat(crimeVersions).hasSize(1)

      // Check that notification to start algo was generated
      assertThat(getNumberOfMessagesCurrentlyOnMatchingNotificationsQueue()).isEqualTo(1)
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

    fun getNumberOfMessagesCurrentlyOnMatchingNotificationsQueue(): Int = matchingNotificationsSqsClient.countAllMessagesOnQueue(
      matchingNotificationsSqsUrl,
    ).get()

    fun getMessage(objectKey: String): String = """
        {
          "Type" : "Notification",
          "MessageId" : "4730435b-88b9-5b6c-a91c-9b1236b456f7",
          "TopicArn" : "arn:aws:sns:eu-west-2:000000000000:email-topic",
          "Message" : "{ \"notificationType\": \"Received\", \"receipt\": { \"action\": { \"bucketName\": \"$BUCKET_NAME\", \"objectKey\": \"$objectKey\" }}}"
        }
    """.trimIndent()
  }
}
