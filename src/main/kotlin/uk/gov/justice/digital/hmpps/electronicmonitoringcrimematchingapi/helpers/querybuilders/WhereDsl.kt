package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders

import java.time.ZonedDateTime

class WhereDsl {
  internal val conditions = mutableListOf<String>()
  internal val values = mutableListOf<String>()

  infix fun String.eq(value: String) {
    conditions.add("$this = ?")
    values.add(value)
  }

  fun or(block: WhereDsl.() -> Unit) {
    val nested = WhereDsl()
    nested.block()
    if (nested.conditions.isNotEmpty()) {
      conditions.add("(" + nested.conditions.joinToString(" OR ") + ")")
      values.addAll(nested.values)
    }
  }

  infix fun String.like(value: String) {
    conditions.add("$this LIKE ?")
    values.add("%$value%")
  }

  fun likeCast(field: String, value: String) {
    conditions.add("CAST($field AS VARCHAR) LIKE ?")
    values.add("%$value%")
  }

  infix fun String.gte(value: ZonedDateTime) {
    conditions.add("$this >= from_iso8601_timestamp(?)")
    values.add(value.toString())
  }

  infix fun String.lte(value: ZonedDateTime) {
    conditions.add("$this <= from_iso8601_timestamp(?)")
    values.add(value.toString())
  }
}