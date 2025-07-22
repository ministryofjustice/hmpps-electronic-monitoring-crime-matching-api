package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

fun nullableLocalDateTime(date: String?): LocalDateTime? = if (!date.isNullOrEmpty()) LocalDateTime.parse(date, formatter) else null
