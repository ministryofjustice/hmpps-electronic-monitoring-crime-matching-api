package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.statements

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.Column
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.ColumnSet
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.SelectStatement

class Select(columnSet: ColumnSet, val fields: List<Column<*>>) : SelectStatement(columnSet) {
  override fun toString(): String {
    if (fields.isEmpty()) {
      throw Exception("Cannot prepare SELECT without columns")
    }

    val builder = StringBuilder("SELECT ")

    fields.forEachIndexed { index, column ->
      builder.append(column)
      if (index < fields.size - 1) {
        builder.append(", ")
      }
    }

    builder.append(" FROM $columnSet")

    return builder.toString()
  }
}
