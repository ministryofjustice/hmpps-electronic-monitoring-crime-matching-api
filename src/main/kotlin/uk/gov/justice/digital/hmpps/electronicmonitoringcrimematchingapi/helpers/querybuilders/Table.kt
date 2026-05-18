package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders

import java.time.ZonedDateTime

open class Table(val name: String) : ColumnSet() {
  private val _columns = mutableListOf<Column<*>>()

  val columns: List<Column<*>> get() = _columns

  fun integer(name: String): Column<Int> = registerColumn(name)

  fun long(name: String): Column<Long> = registerColumn(name)

  fun double(name: String): Column<Double> = registerColumn(name)

  fun varchar(name: String): Column<String> = registerColumn(name)

  fun date(name: String): Column<ZonedDateTime> = registerColumn(name)

  private fun <T> registerColumn(name: String): Column<T> = Column<T>(this, name).also { _columns.add(it) }

  override fun toString() = name

  fun aliased(alias: String): AliasedTable = AliasedTable(this, alias)

  open fun ref(): String = name
}
