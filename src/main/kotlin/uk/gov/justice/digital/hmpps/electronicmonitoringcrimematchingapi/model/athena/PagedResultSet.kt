package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena

import software.amazon.awssdk.services.athena.model.ResultSet

data class PagedResultSet(
  val resultSet: ResultSet,
  val pageCount: Int,
)
