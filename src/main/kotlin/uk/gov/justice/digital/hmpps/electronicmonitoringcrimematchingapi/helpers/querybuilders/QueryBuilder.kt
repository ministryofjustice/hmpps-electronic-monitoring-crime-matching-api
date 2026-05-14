package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders

import org.springframework.web.servlet.function.RequestPredicates.param
import java.time.ZonedDateTime

class Column<T>(val table: Table, val name: String)

open class Table(val name: String) {
  private val _columns = mutableListOf<Column<*>>()

  val columns: List<Column<*>> get() = _columns

  fun selectAll(): Query = Query(this)

  fun integer(name: String): Column<Int> = registerColumn(name)

  fun long(name: String): Column<Long> = registerColumn(name)

  fun varchar(name: String): Column<String> = registerColumn(name)

  fun date(name: String): Column<ZonedDateTime> = registerColumn(name)

  private fun <T> registerColumn(name: String): Column<T> = Column<T>(this, name).also { _columns.add(it) }
}

class Query(val table: Table) {
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

    condition
      ?.toString()
      ?.takeIf { it.isNotBlank() }
      ?.let { builder.append(" WHERE ").append(it) }

    return builder.toString()
  }

  private var condition: Condition? = null

  fun where(block: Condition.() -> Unit): Query {
    condition = And().apply(block)
    return this
  }
}

abstract class Condition {
  abstract fun addCondition(condition: Condition)

  fun and(block: Condition.() -> Unit) {
    addCondition(And().apply(block))
  }

  fun or(block: Condition.() -> Unit) {
    addCondition(Or().apply(block))
  }

  infix fun <T> Column<T>.eq(value: T?) {
    addCondition(Eq(this, Parameter(value)))
  }

  infix fun <T> Column<T>.eq(value: Expression<T?>) {
    addCondition(Eq(this, value))
  }

  infix fun <T> Column<T>.gte(value: T) {
    addCondition(Gte(this, Parameter(value)))
  }

  infix fun <T> Column<T>.gte(value: Expression<T>) {
    addCondition(Gte(this, value))
  }
}

open class CompositeCondition(private val op: String) : Condition() {
  private val conditions = mutableListOf<Condition>()

  override fun addCondition(condition: Condition) {
    conditions += condition
  }

  override fun toString(): String = when (conditions.size) {
    0 -> ""
    1 -> conditions.first().toString()
    else -> conditions.joinToString(
      prefix = "(",
      postfix = ")",
      separator = " $op ",
    )
  }
}

class And : CompositeCondition("AND")
class Or : CompositeCondition("OR")

abstract class Expression<T> {
  abstract fun parameters(): List<T?>
}

class Parameter<T>(private val param: T?) : Expression<T>() {
  override fun toString() = "?"

  override fun parameters() = listOf(param)
}

open class Function<T>(private val name: String, private val args: List<Expression<T>>) : Expression<T>() {
  override fun toString() = "$name(${args.joinToString(", ") { it.toString() }})"
  override fun parameters() = args.flatMap { it.parameters() }
}

fun fromIso8601Timestamp(timestamp: ZonedDateTime): Function<ZonedDateTime> = Function(
  "from_iso8601_timestamp",
  listOf(
    Parameter(timestamp),
  ),
)

class Eq<T>(private val column: Column<T>, private val value: Expression<T?>) : Condition() {
  override fun addCondition(condition: Condition): Unit = throw IllegalStateException("Can't add a nested condition to the eq Operator")

  override fun toString(): String = when (value) {
    null -> "${column.name} is null"
    else -> "${column.name} = $value"
  }
}

class Gte<T>(private val column: Column<T>, private val value: Expression<T>) : Condition() {
  override fun addCondition(condition: Condition): Unit = throw IllegalStateException("Can't add a nested condition to the gte Operator")

  override fun toString(): String = "${column.name} >= $value"
}
