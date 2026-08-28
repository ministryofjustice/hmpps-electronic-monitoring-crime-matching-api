package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.expressions

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.Expression
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.SqlType

class Cast<T>(
  private val expression: Expression<*>,
  private val type: SqlType<T>,
) : Expression<T>() {
  override fun parameters() = expression.parameters()

  override fun toString() = "CAST($expression AS $type)"
}
