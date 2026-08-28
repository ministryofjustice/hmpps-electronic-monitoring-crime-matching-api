package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders

open class Column<T>(val table: Table, val name: String) : Expression<T>() {
  override fun parameters() = emptyList<String>()

  override fun toString(): String = "${table.ref()}.$name"
}
