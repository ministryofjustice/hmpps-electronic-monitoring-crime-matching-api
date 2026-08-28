package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.expressions

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.Expression

class Lower(private val expression: Expression<String>) : Expression<String>() {
  override fun parameters(): List<String> = expression.parameters()

  override fun toString(): String = "LOWER($expression)"
}
