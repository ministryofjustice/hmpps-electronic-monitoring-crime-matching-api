package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.application.dto

data class CreateContextualReportResponse(
  val documentRequestId: String,
  val deviceWearerId: String,
)
