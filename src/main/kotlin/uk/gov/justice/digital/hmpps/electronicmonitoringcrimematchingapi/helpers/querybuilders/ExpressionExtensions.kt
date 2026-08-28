package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.expressions.Cast
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.expressions.Lower

object ExpressionExtensions {
  fun <T> Expression<*>.cast(type: SqlType<T>): Cast<T> = Cast(this, type)

  fun Expression<String>.lower(): Lower = Lower(this)
}
