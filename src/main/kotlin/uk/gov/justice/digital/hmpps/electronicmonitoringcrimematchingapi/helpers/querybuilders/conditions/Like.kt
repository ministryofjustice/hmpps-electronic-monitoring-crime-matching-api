package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.conditions

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.Condition
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.Expression

class Like(private val left: Expression, private val right: Expression) : Condition() {
  override fun addCondition(condition: Condition) = throw IllegalStateException("Can't add a nested condition to the like operator")

  override fun parameters(): List<String> = right.parameters()

  override fun toString(): String = "$left LIKE $right"
}
