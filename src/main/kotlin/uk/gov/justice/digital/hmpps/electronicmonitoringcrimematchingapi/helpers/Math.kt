package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers

import kotlin.math.pow
import kotlin.math.round

fun Double.roundTo(dp: Int): Double {
  val factor = 10.0.pow(dp)
  return round(this * factor) / factor
}
