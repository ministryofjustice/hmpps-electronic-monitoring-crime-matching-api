package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.conditions.And
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.AthenaQuery

class Query(val table: Table) {
  private var condition: Condition? = null

  fun prepare(): AthenaQuery {
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

    return AthenaQuery(
      queryString = builder.toString(),
      parameters = (condition?.parameters() ?: emptyList()).toTypedArray(),
    )
  }

  fun where(block: Condition.() -> Unit): Query {
    condition = And().apply(block)
    return this
  }
}
