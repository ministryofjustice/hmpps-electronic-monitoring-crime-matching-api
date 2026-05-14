package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.functions

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.expressions.Function
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.expressions.Parameter
import java.time.ZonedDateTime

object AthenaFunctions {
  fun fromIso8601Timestamp(timestamp: ZonedDateTime): Function = Function(
    "from_iso8601_timestamp",
    listOf(
      Parameter(timestamp),
    ),
  )
}
