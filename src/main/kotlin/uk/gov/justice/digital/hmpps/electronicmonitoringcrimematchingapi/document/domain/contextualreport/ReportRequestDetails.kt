package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport

import java.time.Instant

data class ReportRequestDetails(
  val policeForceArea: String,
  val requestedAt: Instant,
  val requestedBy: String,
) {
  init {
    require(policeForceArea.isNotBlank()) {
      "'policeForceArea' cannot be blank"
    }

    require(requestedBy.isNotBlank()) {
      "'requestedBy' cannot be blank"
    }
  }
}
