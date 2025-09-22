package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.client.EmDatastoreClientInterface
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.AthenaResultSetExtractor
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.AthenaQuery

abstract class AthenaRepository<T>(
  private val athenaClient: EmDatastoreClientInterface,
) {
  abstract val resultSetExtractor: AthenaResultSetExtractor<T>

  fun executeQuery(query: AthenaQuery): List<T> {
    val queryExecutionId = athenaClient.getQueryExecutionId(query)
    val queryResult = athenaClient.getQueryResult(queryExecutionId)

    return resultSetExtractor.extractData(queryResult)
  }
}
