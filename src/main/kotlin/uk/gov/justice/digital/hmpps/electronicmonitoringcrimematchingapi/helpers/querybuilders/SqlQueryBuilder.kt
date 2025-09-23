package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders

import io.zeko.db.sql.Query
import io.zeko.db.sql.QueryBlock
import io.zeko.db.sql.dsl.eq
import io.zeko.db.sql.dsl.greaterEq
import io.zeko.db.sql.dsl.lessEq
import io.zeko.db.sql.dsl.like
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.AthenaQuery
import java.time.ZonedDateTime

enum class JoinType {
  INNER,
  LEFT,
  RIGHT,
  FULL,
}

data class Join(
  val table: String,
  val onClause: String,
  val type: JoinType = JoinType.INNER,
)

open class SqlQueryBuilder(
  private val baseTable: String,
  private val baseAlias: String = "t",
) {
  private val joins: MutableList<Join> = mutableListOf()
  private val fields: MutableList<String> = mutableListOf()
  private val whereClauses: MutableList<QueryBlock> = mutableListOf()
  private val values: MutableList<String> = mutableListOf()

  fun addFields(fieldList: List<String>): SqlQueryBuilder = apply {
    fields.addAll(fieldList)
  }

  fun addJoin(table: String, onClause: String, type: JoinType): SqlQueryBuilder = apply {
    joins.add(Join(table, onClause, type))
  }

  fun addLikeFilter(field: String, value: String?): SqlQueryBuilder = apply {
    if (!value.isNullOrBlank()) {
      values.add("'%$value%'")
      whereClauses.add(field like "'%$value%'")
    }
  }

  fun addFilter(field: String, value: String): SqlQueryBuilder = apply {
    values.add("'$value'")
    whereClauses.add(field eq "'$value'")
  }

  fun addFilter(field: String, value: Int): SqlQueryBuilder = apply {
    values.add("$value")
    whereClauses.add(field eq "$value")
  }

  fun addFilter(field: String, value: Long): SqlQueryBuilder = apply {
    values.add("$value")
    whereClauses.add(field eq "$value")
  }

  fun greaterEq(field: String, value: ZonedDateTime): SqlQueryBuilder = apply {
    values.add("'$value'")
    whereClauses.add(field greaterEq "from_iso8601_timestamp(?)")
  }

  fun lessEq(field: String, value: ZonedDateTime): SqlQueryBuilder = apply {
    values.add("'$value'")
    whereClauses.add(field lessEq "from_iso8601_timestamp(?)")
  }

  fun addLikeFilterCast(field: String, value: String?): SqlQueryBuilder = apply {
    if (!value.isNullOrBlank()) {
      values.add("'%$value%'")
      whereClauses.add("CAST($field AS VARCHAR)" like "'%$value%'")
    }
  }

  fun build(): AthenaQuery {
    val query = Query().from("$baseTable $baseAlias")

    joins.forEach { join ->
      when (join.type) {
        JoinType.INNER -> query.innerJoin(join.table).on(join.onClause)
        JoinType.LEFT -> query.leftJoin(join.table).on(join.onClause)
        JoinType.RIGHT -> query.rightJoin(join.table).on(join.onClause)
        JoinType.FULL -> query.fullJoin(join.table).on(join.onClause)
      }
    }

    query.fields(*fields.toTypedArray())

    whereClauses.forEach { query.where(it) }

    return AthenaQuery(query.toSql(), values.toTypedArray())
  }
}
