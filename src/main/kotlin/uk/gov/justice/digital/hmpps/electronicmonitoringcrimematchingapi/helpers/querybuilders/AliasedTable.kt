package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders

class AliasedTable(val source: Table, val alias: String) : Table(source.name) {

  override fun ref(): String = alias

  override fun toString() = "$name $alias"

  @Suppress("UNCHECKED_CAST")
  operator fun <T> get(column: Column<T>): Column<T> = source.columns.associate { column ->
    column.name to Column<T>(this, column.name)
  }[column.name] as Column<T>
}
