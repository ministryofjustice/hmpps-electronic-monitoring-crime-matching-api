package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal

import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.mail.util.MimeMessageParser
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.ResponseInputStream
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatch
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeBatchRepository
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties
import javax.mail.Session
import javax.mail.internet.MimeMessage

// TODO Separate credentials provider needed to avoid conflict with datastore setup
// TODO sort out localstack vs dev setup etc
// TODO more error handling throughout for various failure points (+Logging)

@Service
class EmailListener(
  private val mapper: ObjectMapper,
  private val repository: CrimeBatchRepository,
) {

  @SqsListener("email", factory = "hmppsQueueContainerFactoryProxy")
  fun receiveEmailNotification(message: SqsMessage) {
    //Receive SQS message and map to common object with relevant fields
    val mappedMessage = mapper.readValue(message.Message, MessageBody::class.java)

    // Get S3 object key and bucket from message
    val s3Bucket = mappedMessage.receipt.action.bucketName
    val objectKey = mappedMessage.receipt.action.objectKey

    //Parse S3 email object (Mime Format > Get CSV)
    val emailFile = getEmailFileFromS3(s3Bucket, objectKey)

    //Get attachment as bytes
    val csvData = emailFile.use { extractAttachment(it) }

    //Parse csv rows and insert into DB
    parseCsv(csvData)
  }

  private fun createLocalStackS3Client(): S3Client {
    val s3Configuration = S3Configuration.builder().pathStyleAccessEnabled(true).build()

    return S3Client.builder()
      .endpointOverride(java.net.URI("http://localhost:4566"))
      .serviceConfiguration(s3Configuration)
      .build()
  }

  private fun getEmailFileFromS3(bucket: String, objectKey: String): InputStream {
    val s3Client = createLocalStackS3Client()

    val objectRequest = GetObjectRequest
      .builder()
      .key(objectKey)
      .bucket(bucket)
      .build()

    try {
      val s3ObjectAlt: ResponseInputStream<GetObjectResponse> = s3Client.getObject(objectRequest)
      return s3ObjectAlt
    } catch (e: Exception) {
      throw IOException("Failed to retrieve S3 object: $objectKey")
    }
  }

  private fun extractAttachment(emailFile: InputStream): ByteArray {
    val session = Session.getDefaultInstance(Properties())
    val mimeMessage = MimeMessage(session, emailFile)

    val parser = MimeMessageParser(mimeMessage).parse()

    val csvData = parser.attachmentList.first() {
        attachment -> attachment.name.endsWith(".csv")
    }

    return csvData?.inputStream?.use { it.readBytes() } ?: throw NoSuchElementException("No CSV attachment found in email")
  }

  private fun parseCsv(csvData: ByteArray) {
    val reader = InputStreamReader(csvData.inputStream())

    val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    val formattedDate = LocalDateTime.now().format(formatter)
    val batchId = "PFA$formattedDate"

    reader.use {
      val csvFormat = CSVFormat.DEFAULT.builder()
        .setTrim(true)
        .get()

      val parser = CSVParser.parse(it, csvFormat)

      for (record in parser) {
        val crimeBatch = CrimeBatch(
          policeForce = record[0],
          crimeTypeId = record[1],
          crimeTypeDescription = record[2],
          batchId = record[3],
          crimeId = record[4]
        )
        repository.save(crimeBatch)
      }
    }
  }

}