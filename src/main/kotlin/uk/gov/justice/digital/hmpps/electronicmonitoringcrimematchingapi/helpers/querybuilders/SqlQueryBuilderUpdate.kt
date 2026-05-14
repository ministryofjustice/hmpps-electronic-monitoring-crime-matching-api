package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.AthenaQuery

open class SqlQueryBuilderUpdate(
  private val baseTable: String,
  private val alias: String = "t",
) {

  private val fields = mutableListOf<String>()
  private val joins = mutableListOf<Join>()
  private val conditions = mutableListOf<String>()
  private val values = mutableListOf<String>()

  fun select(vararg cols: String) = apply {
    fields.addAll(cols)
  }

  fun join(
    table: String,
    on: String,
    type: JoinType = JoinType.INNER
  ) = apply {
    joins.add(Join(table, on, type))
  }

  fun where(block: WhereDsl.() -> Unit) = apply {
    val dsl = WhereDsl()
    dsl.block()
    conditions.addAll(dsl.conditions)
    values.addAll(dsl.values)
  }

  fun build(): AthenaQuery {
    val sql = buildString {
      append("SELECT ")
      append(if (fields.isEmpty()) "*" else fields.joinToString(", "))
      append(" FROM $baseTable $alias ")

      joins.forEach {
        append(
          when (it.type) {
            JoinType.INNER -> "INNER JOIN"
            JoinType.LEFT -> "LEFT JOIN"
            JoinType.RIGHT -> "RIGHT JOIN"
            JoinType.FULL -> "FULL JOIN"
          }
        )
        append(" ${it.table} ON ${it.onClause} ")
      }

      if (conditions.isNotEmpty()) {
        append(" WHERE ")
        append(conditions.joinToString(" AND "))
      }
    }

    return AthenaQuery(sql.trim(), values.toTypedArray())
  }
}
