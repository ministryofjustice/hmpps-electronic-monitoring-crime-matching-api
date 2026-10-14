package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestParameters
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentType

data class ContextualReportRequestParameters(
  val additionalInformation: String,
  val authorisingManager: AuthorisingManagerDetails,
  val crime: CrimeDetails,
  val deviceWearer: DeviceWearerDetails,
  val positionSegments: List<List<Position>>,
  val reportPeriod: ReportPeriod,
  val reportRequest: ReportRequestDetails,
) : DocumentRequestParameters {
  override val type = DocumentType.CONTEXTUAL_REPORT

  init {
    require(reportRequest.requestedAt.isAfter(reportPeriod.end)) {
      "'reportPeriod.end' must be before 'reportRequest.requestedAt'"
    }

    require(positionSegments.isNotEmpty()) {
      "'positionSegments' must not be empty"
    }
  }
}
