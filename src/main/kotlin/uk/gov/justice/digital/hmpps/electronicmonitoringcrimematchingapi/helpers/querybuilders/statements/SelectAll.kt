package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.statements

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.ColumnSet
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.SelectStatement

class SelectAll(columnSet: ColumnSet) : SelectStatement(columnSet) {
  override fun toString(): String = "SELECT * FROM $columnSet"
}
