package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain

import java.time.Instant

data class ContextualReportRequestParameters(
  val deviceId: Int,
  val from: Instant,
  val to: Instant,
) : DocumentRequestParameters {
  override val type = DocumentType.CONTEXTUAL_REPORT

  init {
    require(deviceId > 0) {
      "'deviceId' must be a positive integer"
    }

    require(from.isBefore(to)) {
      "'from' must be before 'to'"
    }
  }
}
