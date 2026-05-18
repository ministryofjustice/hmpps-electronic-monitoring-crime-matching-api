package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders

class JoinTable(
  private val left: ColumnSet,
  private val right: ColumnSet,
  private val type: JoinType,
  private val on: Condition,
) : ColumnSet() {
  override fun toString(): String = "$left $type JOIN $right ON $on"
}
