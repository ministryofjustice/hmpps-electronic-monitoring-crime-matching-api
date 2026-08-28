package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.expressions.Lower

object ColumnExtensions {
  fun <T> Expression<*>.cast(type: SqlType<T>): CastColumn<T> = CastColumn(this, type)

  fun <T> Expression<T>.lower(): Lower<T> = Lower(this)
}
