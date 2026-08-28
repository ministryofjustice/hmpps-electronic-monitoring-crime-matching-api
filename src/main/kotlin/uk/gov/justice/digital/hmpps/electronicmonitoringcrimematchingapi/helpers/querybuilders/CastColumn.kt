package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders

class CastColumn<T>(
  private val expression: Expression<*>,
  private val type: SqlType<T>,
) : Expression<T>() {
  override fun parameters() = expression.parameters()

  override fun toString() = "CAST($expression AS $type)"
}
