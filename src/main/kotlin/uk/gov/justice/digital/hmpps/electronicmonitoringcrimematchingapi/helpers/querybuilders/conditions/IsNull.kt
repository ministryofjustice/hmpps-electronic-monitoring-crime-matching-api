package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.conditions

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.Condition
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.Expression

class IsNull<T>(private val column: Expression<T>) : Condition() {
  override fun addCondition(condition: Condition): Unit = throw IllegalStateException("Can't add a nested condition to the IsNull Operator")

  override fun parameters(): List<String> = emptyList()

  override fun toString(): String = "$column is NULL"
}
