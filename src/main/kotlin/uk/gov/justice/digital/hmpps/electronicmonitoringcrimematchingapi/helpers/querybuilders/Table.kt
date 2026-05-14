package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.statements.Select
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.statements.SelectAll
import java.time.ZonedDateTime

open class Table(val name: String) {
  private val _columns = mutableListOf<Column<*>>()

  val columns: List<Column<*>> get() = _columns

  fun selectAll(): Query = Query(SelectAll(this))

  fun select(columns: List<Column<*>>): Query = Query(Select(this, columns))

  fun integer(name: String): Column<Int> = registerColumn(name)

  fun long(name: String): Column<Long> = registerColumn(name)

  fun varchar(name: String): Column<String> = registerColumn(name)

  fun date(name: String): Column<ZonedDateTime> = registerColumn(name)

  private fun <T> registerColumn(name: String): Column<T> = Column<T>(this, name).also { _columns.add(it) }
}
