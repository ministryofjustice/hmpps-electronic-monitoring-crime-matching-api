package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.client

import org.apache.commons.csv.CSVFormat
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import software.amazon.awssdk.services.athena.AthenaClient
import software.amazon.awssdk.services.athena.model.AthenaException
import software.amazon.awssdk.services.athena.model.Datum
import software.amazon.awssdk.services.athena.model.GetQueryExecutionRequest
import software.amazon.awssdk.services.athena.model.GetQueryExecutionResponse
import software.amazon.awssdk.services.athena.model.GetQueryResultsRequest
import software.amazon.awssdk.services.athena.model.GetQueryResultsResponse
import software.amazon.awssdk.services.athena.model.QueryExecutionContext
import software.amazon.awssdk.services.athena.model.QueryExecutionState
import software.amazon.awssdk.services.athena.model.ResultConfiguration
import software.amazon.awssdk.services.athena.model.ResultSet
import software.amazon.awssdk.services.athena.model.ResultSetMetadata
import software.amazon.awssdk.services.athena.model.Row
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest
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
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config.datastore.DatastoreProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.AthenaQuery
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.PagedResultSet
import java.io.ByteArrayOutputStream
import java.io.StringReader
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@EnableConfigurationProperties(DatastoreProperties::class)
@Component
class EmDatastoreClient(
  val athenaClient: AthenaClient,
  val s3AsyncClient: S3AsyncClient,
  val properties: DatastoreProperties,
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  fun getQueryResult(queryExecutionId: String): ResultSet {
    waitForQueryToComplete(queryExecutionId)
    return retrieveResults(queryExecutionId)
  }

  fun getPagedQueryResult(queryExecutionId: String, page: Int, pageSize: Int): PagedResultSet {
    val queryResponse = waitForQueryToComplete(queryExecutionId)
    val outputUri = URI(queryResponse.queryExecution().resultConfiguration().outputLocation())
    val bucket = outputUri.host
    val key = outputUri.path.removePrefix("/")

    val metadata = retrieveColumnMetadata(queryExecutionId)

    val offset = page * pageSize
    val pageCsv = selectObjectContent(
      bucket = bucket,
      key = key,
      sqlExpression = "SELECT * FROM s3object WHERE CAST(row_number AS INT) > $offset LIMIT $pageSize",
    )
    val pageRows = parseCsvRows(pageCsv)

    val totalRecords = countRows(bucket, key)
    val pageCount = totalRecords / pageSize + if (totalRecords % pageSize == 0L) 0 else 1

    val resultSet = ResultSet.builder()
      .resultSetMetadata(metadata)
      .rows(listOf(buildHeaderRow(metadata)) + pageRows)
      .build()

    return PagedResultSet(resultSet, pageCount.toInt())
  }

  @Cacheable("athenaQueryExecutions")
  @Transactional
  fun getQueryExecutionId(athenaQuery: AthenaQuery): String {
    val queryExecutionId: String = submitAthenaQuery(athenaQuery)
    return queryExecutionId
  }

  @Throws(AthenaClientException::class)
  private fun submitAthenaQuery(query: AthenaQuery): String {
    return try {
      val queryExecutionContext = QueryExecutionContext.builder()
        .catalog("AwsDataCatalog")
        .database(properties.database)
        .build()

      // The result configuration specifies where the results of the query should go.
      val resultConfiguration = ResultConfiguration.builder()
        .outputLocation(properties.outputBucketArn)
        .build()

      val startQueryExecutionRequest = StartQueryExecutionRequest.builder()
        .queryString(query.queryString)
        .queryExecutionContext(queryExecutionContext)
        .workGroup(properties.workgroup)

      if (query.parameters.isNotEmpty()) {
        startQueryExecutionRequest.executionParameters(*query.parameters.toTypedArray())
      }

      startQueryExecutionRequest.resultConfiguration(resultConfiguration)

      log.debug("Workgroup: {}", properties.workgroup)
      log.debug("Database: {}", properties.database)
      log.debug("Starting query: {}", query)

      val startQueryExecutionResponse = athenaClient.startQueryExecution(startQueryExecutionRequest.build())

      return startQueryExecutionResponse.queryExecutionId()
    } catch (e: AthenaException) {
      throw AthenaClientException("Error submitting query to Athena: ${e.message}")
    }
  }

  // Wait for an Amazon Athena query to complete, fail or to be cancelled.
  @Throws(InterruptedException::class)
  private fun waitForQueryToComplete(queryExecutionId: String): GetQueryExecutionResponse {
    val getQueryExecutionRequest = GetQueryExecutionRequest.builder()
      .queryExecutionId(queryExecutionId)
      .build()

    while (true) {
      val response = athenaClient.getQueryExecution(getQueryExecutionRequest)
      val queryState = response.queryExecution().status().state()
      log.debug("Query execution id $queryExecutionId has status: $queryState")
      when (queryState) {
        QueryExecutionState.FAILED -> throw RuntimeException(
          "The Amazon Athena query failed to run with error message: " +
            response.queryExecution().status().stateChangeReason(),
        )
        QueryExecutionState.CANCELLED -> throw RuntimeException(
          "The Amazon Athena query was cancelled",
        )
        QueryExecutionState.SUCCEEDED -> return response
        else -> Thread.sleep(properties.retryIntervalMs)
      }
    }
  }

  @Throws(AthenaClientException::class)
  private fun retrieveResults(queryExecutionId: String): ResultSet {
    try {
      val getQueryResultsRequest = GetQueryResultsRequest.builder()
        .queryExecutionId(queryExecutionId)
        .build()

      val queryResults: GetQueryResultsResponse = athenaClient.getQueryResults(getQueryResultsRequest)
      return queryResults.resultSet()
    } catch (e: AthenaException) {
      throw AthenaClientException("Error submitting query to Athena: ${e.message}")
    }
  }

  private fun retrieveColumnMetadata(queryExecutionId: String): ResultSetMetadata {
    val response = athenaClient.getQueryResults(
      GetQueryResultsRequest.builder()
        .queryExecutionId(queryExecutionId)
        .maxResults(1)
        .build(),
    )
    return response.resultSet().resultSetMetadata()
  }

  private fun selectObjectContent(bucket: String, key: String, sqlExpression: String): String {
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

  private fun countRows(bucket: String, key: String): Long {
    val csv = selectObjectContent(bucket, key, "SELECT COUNT(*) FROM s3object")
    return csv.trim().lines().first().trim().toLong()
  }

  private fun parseCsvRows(csv: String): List<Row> = CSVFormat.DEFAULT.parse(StringReader(csv)).records.map { record ->
    Row.builder()
      .data(record.map { value -> Datum.builder().varCharValue(value).build() })
      .build()
  }

  private fun buildHeaderRow(metadata: ResultSetMetadata): Row = Row.builder()
    .data(metadata.columnInfo().map { Datum.builder().varCharValue(it.name()).build() })
    .build()
}
