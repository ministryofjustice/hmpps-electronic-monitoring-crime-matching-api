package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.client

import org.springframework.stereotype.Component
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.CSVInput
import software.amazon.awssdk.services.s3.model.CSVOutput
import software.amazon.awssdk.services.s3.model.CompressionType
import software.amazon.awssdk.services.s3.model.ExpressionType
import software.amazon.awssdk.services.s3.model.FileHeaderInfo
import software.amazon.awssdk.services.s3.model.InputSerialization
import software.amazon.awssdk.services.s3.model.OutputSerialization
import software.amazon.awssdk.services.s3.model.RecordsEvent
import software.amazon.awssdk.services.s3.model.SelectObjectContentRequest
import software.amazon.awssdk.services.s3.model.SelectObjectContentResponseHandler
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config.AthenaClientException
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@Component
class AwsS3SelectReader(
  private val s3AsyncClient: S3AsyncClient,
) : S3SelectReader {

  override fun selectObjectContent(bucket: String, key: String, sqlExpression: String): String {
    val request = SelectObjectContentRequest.builder()
      .bucket(bucket)
      .key(key)
      .expressionType(ExpressionType.SQL)
      .expression(sqlExpression)
      .inputSerialization(
        InputSerialization.builder()
          .csv(CSVInput.builder().fileHeaderInfo(FileHeaderInfo.USE).build())
          .compressionType(CompressionType.NONE)
          .build(),
      )
      .outputSerialization(
        OutputSerialization.builder()
          .csv(CSVOutput.builder().build())
          .build(),
      )
      .build()

    val output = ByteArrayOutputStream()
    val handler = SelectObjectContentResponseHandler.builder()
      .subscriber { event ->
        event.accept(
          object : SelectObjectContentResponseHandler.Visitor {
            override fun visitRecords(recordsEvent: RecordsEvent) {
              output.write(recordsEvent.payload().asByteArray())
            }
          },
        )
      }
      .onError { throw AthenaClientException("Error selecting object content: ${it.message}") }
      .build()

    val future = s3AsyncClient.selectObjectContent(request, handler)
    try {
      future.get(60, TimeUnit.SECONDS)
    } catch (e: InterruptedException) {
      future.cancel(true)
      Thread.currentThread().interrupt()
      throw AthenaClientException("Interrupted while waiting for S3 Select")
    } catch (e: ExecutionException) {
      throw AthenaClientException("Error selecting object content: ${e.cause?.message ?: e.message}")
    } catch (e: TimeoutException) {
      future.cancel(true)
      throw AthenaClientException("Timeout while waiting for S3 Select")
    }

    return output.toString(StandardCharsets.UTF_8)
  }
}
