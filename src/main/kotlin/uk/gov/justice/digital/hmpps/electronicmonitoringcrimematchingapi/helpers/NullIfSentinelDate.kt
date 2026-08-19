package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers

private const val DEVICE_DEACTIVATION_DATE_SENTINEL = "2999-12-12 23:59:59.000"

fun nullIfSentinelDate(value: String?): String? = value
  ?.takeUnless { it == DEVICE_DEACTIVATION_DATE_SENTINEL }
