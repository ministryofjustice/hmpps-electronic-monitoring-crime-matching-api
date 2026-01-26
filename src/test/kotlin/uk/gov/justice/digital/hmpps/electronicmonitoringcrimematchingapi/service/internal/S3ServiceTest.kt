package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.test.context.ActiveProfiles
import software.amazon.awssdk.core.ResponseInputStream
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.S3Exception
import java.io.ByteArrayInputStream
import java.io.IOException

@ActiveProfiles("test")
class S3ServiceTest {
  private lateinit var service: S3Service
  private lateinit var client: S3Client

  @BeforeEach
  fun setup() {
    client = Mockito.mock(S3Client::class.java)
    service = S3Service(client)
  }

  @Nested
  @DisplayName("GetObject")
  inner class Get {
    @Test
    fun `it should return an object from s3`() {
      val fileData = "test"
      val responseStream = ResponseInputStream(
        GetObjectResponse.builder().build(),
        ByteArrayInputStream(fileData.toByteArray()),
      )
      whenever(
        client.getObject(any<GetObjectRequest>()),
      ).thenReturn(responseStream)

      val res = service.getObject("messageId", "objectKey", "bucketName")

      assert(res.readAllBytes().decodeToString().contentEquals(fileData))
    }
  }

  @Test
  fun `it should throw an IOException when the file cannot be read from s3`() {
    whenever(
      client.getObject(any<GetObjectRequest>()),
    ).thenThrow(S3Exception.builder().build())

    val exception = assertThrows<IOException> {
      service.getObject("messageId", "objectKey", "bucketName")
    }

    assertEquals("Message messageId failed to retrieve S3 object objectKey from bucket bucketName due to: null", exception.message)
  }
}
