package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.conditions

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.Column
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.Condition
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.Expression

class LessThanEqual<T>(private val column: Column<T>, private val value: Expression) : Condition() {
  override fun addCondition(condition: Condition): Unit = throw IllegalStateException("Can't add a nested condition to the gte Operator")

  override fun parameters(): List<String> = value.parameters()

  override fun toString(): String = "$column <= $value"
}
