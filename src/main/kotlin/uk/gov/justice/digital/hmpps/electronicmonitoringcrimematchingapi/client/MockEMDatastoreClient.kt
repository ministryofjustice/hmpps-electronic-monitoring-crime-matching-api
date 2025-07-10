package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.client

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.athena.model.ResultSet
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.AthenaHelper
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.AthenaQuery
import java.nio.file.Paths

@Component
@Profile("mocking")
class MockEMDatastoreClient : EmDatastoreClientInterface {
  private companion object {
    private const val MOCKS_RESOURCE_PATH = "mockAthenaResponses"

    private const val MOCK_QUERY_EXECUTION_ID = "mock-query-execution-id"

    private val log = LoggerFactory.getLogger(this::class.java)

    private var responses: MutableMap<String, String> = mutableMapOf<String, String>()

    private fun loadResponses() {
      val classLoader = Thread.currentThread().contextClassLoader
      val resourceUrl = classLoader.getResource(MOCKS_RESOURCE_PATH) ?: throw Exception("Resource not found: $MOCKS_RESOURCE_PATH")
      val resourceUri = resourceUrl.toURI()
      val baseDir = Paths.get(resourceUri).toFile()

      val scenarios = baseDir.listFiles()?.map { it.nameWithoutExtension } ?: throw Exception("Files not found")

      scenarios.forEach { scenario ->
        val queryPath = "$MOCKS_RESOURCE_PATH/$scenario/query.sql"
        val responsePath = "$MOCKS_RESOURCE_PATH/$scenario/response.json"

        val rawQuery = classLoader.getResource(queryPath)?.readText(Charsets.UTF_8) ?: return
        val query = stripWhitespace(rawQuery)

        val response = classLoader.getResource(responsePath)?.readText(Charsets.UTF_8) ?: return

        responses[query] = response
      }
    }

    private fun stripWhitespace(value: String): String = value.lines()
      .joinToString(" ") { line -> line.trim() }
      .trimIndent()
  }

  override fun getQueryResult(queryExecutionId: String): ResultSet {
    if (queryExecutionId == "THROW ERROR") {
      throw IllegalArgumentException("I threw an error")
    }

    if (queryExecutionId != MOCK_QUERY_EXECUTION_ID) {
      log.info(
        """
          No response defined for query execution ID $queryExecutionId
        """.trimIndent(),
      )
    }

    val classLoader = Thread.currentThread().contextClassLoader
    val response = classLoader.getResource("$MOCKS_RESOURCE_PATH/successfulSubjectSearch/response.json")?.readText(Charsets.UTF_8) ?: "null"
    val athenaResponse = response.trimIndent()

    return AthenaHelper.resultSetFromJson(athenaResponse)
  }

  override fun getQueryResult(athenaQuery: AthenaQuery): ResultSet {
    if (athenaQuery.queryString == "THROW ERROR") {
      throw IllegalArgumentException("I threw an error")
    }

    if (responses.isEmpty()) {
      loadResponses()
    }

    val query = stripWhitespace("${athenaQuery.queryString}${athenaQuery.parameters.joinToString(",")}")

    val athenaResponse = responses[query]?.trimIndent()
    if (athenaResponse == null) {
      log.info(
        """
          No response defined for query
          -------------
          $query
          -------------
        """.trimIndent(),
      )
    }

    return AthenaHelper.resultSetFromJson(athenaResponse!!)
  }

  override fun getQueryExecutionId(athenaQuery: AthenaQuery): String {
    if (athenaQuery.queryString == "THROW ERROR") {
      throw IllegalArgumentException("I threw an error")
    }
    if (responses.isEmpty()) {
      loadResponses()
    }

    val query = stripWhitespace(athenaQuery.queryString)

    val athenaResponse = responses[query]?.trimIndent()
    if (athenaResponse == null) {
      log.info(
        """
          No response defined for query
          -------------
          ${athenaQuery.queryString}
          -------------
        """.trimIndent(),
      )
    }
    return MOCK_QUERY_EXECUTION_ID
  }
}
