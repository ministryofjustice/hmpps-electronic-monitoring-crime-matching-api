package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.conditions

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.Condition

open class CompositeCondition(private val op: String) : Condition() {
  private val conditions = mutableListOf<Condition>()

  override fun addCondition(condition: Condition) {
    conditions += condition
  }

  override fun parameters(): List<String> = conditions.flatMap { it.parameters() }

  override fun toString(): String = when (conditions.size) {
    0 -> ""
    1 -> conditions.first().toString()
    else -> conditions.joinToString(
      prefix = "( ",
      postfix = " )",
      separator = " $op ",
    )
  }
}
