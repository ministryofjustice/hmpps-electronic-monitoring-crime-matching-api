package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders

class CastColumn<T>(
  private val column: Column<*>,
  private val type: SqlType,
) : Column<T>(column.table, column.name) {
  override fun toString(): String = "CAST($column AS $type)"
}
