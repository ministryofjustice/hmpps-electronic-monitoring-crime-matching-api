package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.expressions

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.Expression
import java.time.ZonedDateTime

class Parameter<T>(private val param: T) : Expression() {
  private fun format(value: Any?): String = when (value) {
    is String ->
      "'${value.replace("'", "''")}'"

    is ZonedDateTime ->
      "'$value'"

    else ->
      value.toString()
  }

  override fun parameters() = listOf(format(param))

  override fun toString() = "?"
}
