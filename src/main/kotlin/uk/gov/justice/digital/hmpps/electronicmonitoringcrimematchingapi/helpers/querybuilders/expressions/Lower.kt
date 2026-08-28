package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.expressions

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.Expression

class Lower<T>(private val expression: Expression<T>) : Expression<T>() {
  override fun parameters(): List<String> = expression.parameters()

  override fun toString(): String = "LOWER($expression)"
}
