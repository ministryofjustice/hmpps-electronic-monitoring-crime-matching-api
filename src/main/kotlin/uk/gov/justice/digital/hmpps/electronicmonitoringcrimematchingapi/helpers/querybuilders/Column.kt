package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.expressions.Cast

class Column<T>(val table: Table, val name: String) : Expression() {
  override fun parameters() = emptyList<String>()

  override fun toString(): String = "${table.ref()}.$name"

  fun asVarchar(): Expression = Cast(this, "VARCHAR")
}
