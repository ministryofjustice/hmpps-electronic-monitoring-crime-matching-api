package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.client.EmDatastoreClient
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.PagedResult
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.AthenaResultSetExtractor
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.AthenaQuery

abstract class AthenaRepository<T>(
  val athenaClient: EmDatastoreClient,
) {
  abstract val resultSetExtractor: AthenaResultSetExtractor<T>

  fun executeQuery(query: AthenaQuery): List<T> {
    val queryExecutionId = athenaClient.getQueryExecutionId(query)
    val queryResult = athenaClient.getQueryResult(queryExecutionId)

    return resultSetExtractor.extractData(queryResult)
  }

  fun executePagedQuery(query: AthenaQuery, page: Int, pageSize: Int): PagedResult<T> {
    val queryExecutionId = athenaClient.getQueryExecutionId(query)
    val queryResult = athenaClient.getPagedQueryResult(queryExecutionId, page, pageSize)

    return PagedResult(
      data = resultSetExtractor.extractData(queryResult.resultSet),
      pageCount = queryResult.pageCount,
    )
  }
}
