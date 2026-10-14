package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport

import java.time.Instant

data class ReportPeriod(
  val start: Instant,
  val end: Instant,
) {
  init {
    require(start.isBefore(end)) {
      "'reportPeriod.start' must be before 'reportPeriod.end'"
    }
  }
}
