package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.client

import software.amazon.awssdk.services.athena.model.ResultSet
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.AthenaQuery

interface EmDatastoreClientInterface {
  fun getQueryResult(athenaQuery: AthenaQuery, role: AthenaRole?): ResultSet
}
