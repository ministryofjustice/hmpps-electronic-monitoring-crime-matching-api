package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.mocks

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.athena.model.ResultSet
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.client.EmDatastoreClientInterface
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.AthenaHelper
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.AthenaQuery
import java.io.File
import kotlin.io.readText

@Component
@Profile("integration")
class MockEmDatastoreClient : EmDatastoreClientInterface {
  companion object {
    var responses: MutableList<String> = mutableListOf<String>()

    fun addResponseFile(responseFilename: String) {
      this.responses.add(responseFilename)
    }

    fun getNextResponse(): String {
      val responseFilename = "./src/test/resources/athenaResponses/${this.responses.removeFirst()}.json"
      return File(responseFilename).readText(Charsets.UTF_8)
    }

    fun reset() {
      this.responses.clear()
    }
  }

  override fun getQueryExecutionId(athenaQuery: AthenaQuery): String {
    if (athenaQuery.queryString == "THROW ERROR") {
      throw kotlin.IllegalArgumentException("I threw an error")
    }

    return "query-execution-id"
  }

  override fun getQueryResult(athenaQuery: AthenaQuery): ResultSet {
    if (athenaQuery.queryString == "THROW ERROR") {
      throw kotlin.IllegalArgumentException("I threw an error")
    }

    val athenaResponse = getNextResponse()
    return AthenaHelper.Companion.resultSetFromJson(athenaResponse)
  }

  override fun getQueryResult(queryExecutionId: String): ResultSet {
    if (queryExecutionId == "THROW ERROR") {
      throw kotlin.IllegalArgumentException("I threw an error")
    }

    val athenaResponse = getNextResponse()
    return AthenaHelper.Companion.resultSetFromJson(athenaResponse)
  }
}
