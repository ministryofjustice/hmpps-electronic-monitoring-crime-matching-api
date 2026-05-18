package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.expressions

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.Expression

open class Function(private val name: String, private val args: List<Expression>) : Expression() {
  override fun toString() = "$name(${args.joinToString(", ") { it.toString() }})"
  override fun parameters() = args.flatMap { it.parameters() }
}
