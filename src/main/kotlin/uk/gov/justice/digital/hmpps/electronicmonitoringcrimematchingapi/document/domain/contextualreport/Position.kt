package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport

import java.time.Instant

data class Position(
  val direction: Long,
  val gpsDate: Instant,
  val latitude: Double,
  val longitude: Double,
  val precision: Long,
  val speed: Long,
  val sequenceNumber: Int,
)
