package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.crimemapping

import org.joda.time.DateTime

data class CrimeBatchSearchCriteria(
  val fromDateTime: DateTime,
  val toDateTime: DateTime,
  val textSearch: String //PFA or BatchId
)