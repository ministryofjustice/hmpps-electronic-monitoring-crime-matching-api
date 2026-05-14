package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders



class Column<T>(val table: Table, val name: String) {

}

open class Table(val name: String) {
  private val _columns = mutableListOf<Column<*>>()

  val columns: List<Column<*>> get() = _columns

  fun selectAll(): Query {
    return Query(this)
  }

  fun integer(name: String): Column<Int> {
    return registerColumn(name)
  }

  fun long(name: String): Column<Long> {
    return registerColumn(name)
  }

  private fun <T> registerColumn(name: String): Column<T> {
    return Column<T>(this, name).also { _columns.add(it) }
  }
}

class Query(val table: Table) {
  private var _where: String? = null

  fun prepareSQL(): String {
    if (this.table.columns.isEmpty()) {
      throw Exception("Cannot prepare SELECT without columns")
    }

    val builder = StringBuilder()

    builder.append("SELECT ")

    this.table.columns.forEachIndexed { index, column ->
      builder.append(column.name)
      if (index < this.table.columns.size - 1) {
        builder.append(", ")
      }
    }

    builder.append(" FROM ")
    builder.append(this.table.name)

    _where?.let { builder.append(" WHERE ").append(it) }

    return builder.toString()
  }

  fun andWhere(andPart: () -> Expression<*>): Query {
    val expr = andPart().toString()
    _where = if (_where.isNullOrBlank()) {
      expr
    }
    else {
      "$_where AND $expr"
    }
    return this
  }
}

open class Expression<T>(val column: Column<T>, val value: T, val operator: String) {
  override fun toString(): String {
    return "${column.name} $operator ?"
  }
}

class Equals<T>(column: Column<T>, value: T) : Expression<T>(column, value, "=") {

}

infix fun <T> Column<T>.eq(value: T) = Equals(this, value)
