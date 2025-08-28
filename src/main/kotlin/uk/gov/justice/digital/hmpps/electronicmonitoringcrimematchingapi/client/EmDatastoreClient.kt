package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.client

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.athena.AthenaClient
import software.amazon.awssdk.services.athena.model.AthenaException
import software.amazon.awssdk.services.athena.model.GetQueryExecutionRequest
import software.amazon.awssdk.services.athena.model.GetQueryExecutionResponse
import software.amazon.awssdk.services.athena.model.GetQueryResultsRequest
import software.amazon.awssdk.services.athena.model.GetQueryResultsResponse
import software.amazon.awssdk.services.athena.model.QueryExecutionContext
import software.amazon.awssdk.services.athena.model.QueryExecutionState
import software.amazon.awssdk.services.athena.model.ResultConfiguration
import software.amazon.awssdk.services.athena.model.ResultSet
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config.AthenaClientException
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config.EmDatastoreCredentialsProvider
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.AthenaQuery

@Component
@Profile("!mocking & !integration")
class EmDatastoreClient : EmDatastoreClientInterface {
  @Value("\${services.athena.output}")
  private val output: String = "s3://emds-dev-athena-query-results-20240917144028307600000004"
  private val sleepLength: Long = 1000

  @Value("\${services.athena-roles.general}")
  private val iamRole: String = ""

  private fun startClient(): AthenaClient {
    val credentialsProvider: AwsCredentialsProvider = EmDatastoreCredentialsProvider.Companion.getCredentials(iamRole)

    return AthenaClient.builder()
      .region(Region.EU_WEST_2)
      .credentialsProvider(credentialsProvider)
      .build()
  }

  override fun getQueryResult(athenaQuery: AthenaQuery): ResultSet {
    val athenaClient = startClient()

    val queryExecutionId: String = submitAthenaQuery(athenaClient, athenaQuery)

    // Wait for query to complete - blocking
    waitForQueryToComplete(athenaClient, queryExecutionId)

    val resultSet: ResultSet = retrieveResults(athenaClient, queryExecutionId)

    athenaClient.close()
    return resultSet
  }

  override fun getQueryResult(queryExecutionId: String): ResultSet {
    val athenaClient = startClient()

    waitForQueryToComplete(athenaClient, queryExecutionId)
    val resultSet: ResultSet = retrieveResults(athenaClient, queryExecutionId)

    athenaClient.close()
    return resultSet
  }

  override fun getQueryExecutionId(athenaQuery: AthenaQuery): String {
    val athenaClient = startClient()
    val queryExecutionId: String = submitAthenaQuery(athenaClient, athenaQuery)
    athenaClient.close()
    return queryExecutionId
  }

  @Throws(AthenaClientException::class)
  private fun submitAthenaQuery(athenaClient: AthenaClient, query: AthenaQuery): String {
    return try {
      // The QueryExecutionContext allows us to set the database.
      val queryExecutionContext = QueryExecutionContext.builder()
        .catalog("AwsDataCatalog")
        .build()

      // The result configuration specifies where the results of the query should go.
      val resultConfiguration = ResultConfiguration.builder()
        .outputLocation(output)
        .build()

      var startQueryExecutionRequest = StartQueryExecutionRequest.builder()
        .queryString(query.queryString)
        .queryExecutionContext(queryExecutionContext)

      if (query.parameters.isNotEmpty()) {
        startQueryExecutionRequest.executionParameters(*query.parameters)
      }

      startQueryExecutionRequest.resultConfiguration(resultConfiguration)

      val startQueryExecutionResponse = athenaClient.startQueryExecution(startQueryExecutionRequest.build())

      return startQueryExecutionResponse.queryExecutionId()
    } catch (e: AthenaException) {
      throw AthenaClientException("Error submitting query to Athena: ${e.message}")
    }
  }

  // Wait for an Amazon Athena query to complete, fail or to be cancelled.
  @Throws(InterruptedException::class)
  private fun waitForQueryToComplete(athenaClient: AthenaClient, queryExecutionId: String?) {
    val getQueryExecutionRequest = GetQueryExecutionRequest.builder()
      .queryExecutionId(queryExecutionId)
      .build()
    var getQueryExecutionResponse: GetQueryExecutionResponse
    var isQueryStillRunning = true
    while (isQueryStillRunning) {
      getQueryExecutionResponse = athenaClient.getQueryExecution(getQueryExecutionRequest)
      val queryState = getQueryExecutionResponse.queryExecution().status().state().toString()
      if (queryState == QueryExecutionState.FAILED.toString()) {
        throw RuntimeException(
          "The Amazon Athena query failed to run with error message: " + getQueryExecutionResponse
            .queryExecution().status().stateChangeReason(),
        )
      } else if (queryState == QueryExecutionState.CANCELLED.toString()) {
        throw RuntimeException("The Amazon Athena query was cancelled.")
      } else if (queryState == QueryExecutionState.SUCCEEDED.toString()) {
        isQueryStillRunning = false
      } else {
        // Sleep an amount of time before retrying again.
        Thread.sleep(sleepLength)
      }
      println("The current status is: $queryState")
    }
  }

  @Throws(AthenaClientException::class)
  private fun retrieveResults(athenaClient: AthenaClient, queryExecutionId: String?): ResultSet {
    return try {
      val getQueryResultsRequest = GetQueryResultsRequest.builder()
        .queryExecutionId(queryExecutionId)
        .build()

      val queryResults: GetQueryResultsResponse = athenaClient.getQueryResults(getQueryResultsRequest)
      return queryResults.resultSet()
    } catch (e: AthenaException) {
      throw AthenaClientException("Error submitting query to Athena: ${e.message}")
      throw e
    }
  }
}
