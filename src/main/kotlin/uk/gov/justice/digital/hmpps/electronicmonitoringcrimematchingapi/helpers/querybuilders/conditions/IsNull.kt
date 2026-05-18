package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.conditions

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.Column
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.Condition

class IsNull<T>(private val column: Column<T>) : Condition() {
  override fun addCondition(condition: Condition): Unit = throw IllegalStateException("Can't add a nested condition to the IsNull Operator")

  override fun parameters(): List<String> = emptyList()

  override fun toString(): String = "$column is NULL"
}
