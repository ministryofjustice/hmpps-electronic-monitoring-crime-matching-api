package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers

import java.time.LocalDateTime

private val DEVICE_DEACTIVATION_DATE_SENTINEL = LocalDateTime.parse("9999-12-31 00:00:00.000", formatter)

fun nullIfSentinelDate(value: LocalDateTime?): LocalDateTime? = value
  ?.takeUnless { it == DEVICE_DEACTIVATION_DATE_SENTINEL }
