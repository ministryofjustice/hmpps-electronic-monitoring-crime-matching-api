package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.conditions

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.Column
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.Condition
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.Expression

class Like<T>(private val column: Column<T>, private val value: Expression) : Condition() {
  override fun addCondition(condition: Condition) = throw IllegalStateException("Can't add a nested condition to the like operator")

  override fun parameters(): List<String> = value.parameters()

  override fun toString(): String = "$column LIKE $value"
}
