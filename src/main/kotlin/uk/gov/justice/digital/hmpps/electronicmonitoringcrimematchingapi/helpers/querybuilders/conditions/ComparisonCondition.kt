package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.conditions

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.Condition
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.Expression

open class ComparisonCondition(private val left: Expression, private val right: Expression, private val operator: String) : Condition() {
  override fun addCondition(condition: Condition): Unit = throw IllegalStateException("Can't add a nested condition to the Equal Operator")

  override fun parameters(): List<String> = left.parameters() + right.parameters()

  override fun toString(): String = "$left $operator $right"
}
