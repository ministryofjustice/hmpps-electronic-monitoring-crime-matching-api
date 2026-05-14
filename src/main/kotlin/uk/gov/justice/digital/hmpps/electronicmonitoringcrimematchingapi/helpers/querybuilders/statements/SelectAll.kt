package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.statements

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.SelectStatement
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.Table

class SelectAll(table: Table) : SelectStatement(table) {
  override fun toString(): String = "SELECT * FROM ${table.name}"
}
