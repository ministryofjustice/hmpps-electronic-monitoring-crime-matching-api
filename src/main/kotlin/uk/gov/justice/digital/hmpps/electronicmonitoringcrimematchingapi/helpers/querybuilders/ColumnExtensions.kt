package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders

object ColumnExtensions {
  fun <T> Column<T>.cast(type: SqlType): CastColumn<T> = CastColumn(this, type)
}
