package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.expressions

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.Expression

class Cast(private val expression: Expression, private val type: String) : Expression() {
  override fun parameters(): List<String> = expression.parameters()
  override fun toString(): String = "CAST($expression AS $type)"
}
