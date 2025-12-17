package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal

import org.springframework.stereotype.Service
import software.amazon.awssdk.core.ResponseInputStream
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import java.io.IOException

@Service
class S3Service(private val s3Client: S3Client) {

  fun getObject(messageId: String, objectKey: String, bucketName: String): ResponseInputStream<GetObjectResponse> {
    val objectRequest = GetObjectRequest
      .builder()
      .key(objectKey)
      .bucket(bucketName)
      .build()

    try {
      return s3Client.getObject(objectRequest)
    } catch (e: Exception) {
      throw IOException("Message $messageId failed to retrieve S3 object $objectKey from bucket $bucketName due to: ${e.message}")
    }
  }
}
