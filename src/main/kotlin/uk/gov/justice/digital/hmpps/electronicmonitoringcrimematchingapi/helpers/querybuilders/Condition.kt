package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.conditions.And
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.conditions.Equal
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.conditions.GreaterThanEqual
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.conditions.IsNull
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.conditions.LessThanEqual
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.conditions.Like
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.conditions.Or
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.expressions.Parameter

abstract class Condition {
  abstract fun addCondition(condition: Condition)

  abstract fun parameters(): List<String>

  fun and(block: Condition.() -> Unit) {
    addCondition(And().apply(block))
  }

  fun or(block: Condition.() -> Unit) {
    addCondition(Or().apply(block))
  }

  infix fun <T> Column<T>.eq(value: T?) {
    addCondition(
      when (value) {
        null -> IsNull(this)
        else -> Equal(this, Parameter(value))
      },
    )
  }

  infix fun <T> Column<T>.eq(value: Expression) {
    addCondition(Equal(this, value))
  }

  infix fun Expression.like(value: String) {
    addCondition(Like(this, Parameter(value)))
  }

  infix fun <T> Column<T>.gte(value: T) {
    addCondition(GreaterThanEqual(this, Parameter(value)))
  }

  infix fun <T> Column<T>.gte(value: Expression) {
    addCondition(GreaterThanEqual(this, value))
  }

  infix fun <T> Column<T>.lte(value: T) {
    addCondition(LessThanEqual(this, Parameter(value)))
  }

  infix fun <T> Column<T>.lte(value: Expression) {
    addCondition(LessThanEqual(this, value))
  }
}
