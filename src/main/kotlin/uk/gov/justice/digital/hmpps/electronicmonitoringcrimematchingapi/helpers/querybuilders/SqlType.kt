package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders

import java.time.ZonedDateTime

sealed class SqlType<T>(val sql: String) {
  data object Date : SqlType<ZonedDateTime>("DATE")
}
