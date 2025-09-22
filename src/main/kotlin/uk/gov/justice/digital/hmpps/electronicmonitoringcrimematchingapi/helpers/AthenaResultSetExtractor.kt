package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers

import software.amazon.awssdk.services.athena.model.ResultSet

interface AthenaResultSetExtractor<T> {
  fun extractData(resultSet: ResultSet): List<T>
}
