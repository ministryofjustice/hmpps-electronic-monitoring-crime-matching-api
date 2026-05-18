package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.conditions.And
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.statements.Select
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.statements.SelectAll

abstract class ColumnSet {
  fun join(
    other: ColumnSet,
    type: JoinType,
    on: Condition.() -> Unit,
  ): JoinTable = JoinTable(this, other, type, And().apply(on))

  fun selectAll(): Query = Query(SelectAll(this))

  fun select(vararg columns: Column<*>): Query = Query(Select(this, columns.toList()))
}
