package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.statements

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.Column
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.SelectStatement
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.Table

class Select(table: Table, val fields: List<Column<*>>) : SelectStatement(table) {
  override fun toString(): String {
    if (fields.isEmpty()) {
      throw Exception("Cannot prepare SELECT without columns")
    }

    val builder = StringBuilder("SELECT ")

    fields.forEachIndexed { index, column ->
      builder.append(column.name)
      if (index < fields.size - 1) {
        builder.append(", ")
      }
    }

    builder.append(" FROM ${table.name}")

    return builder.toString()
  }
}
