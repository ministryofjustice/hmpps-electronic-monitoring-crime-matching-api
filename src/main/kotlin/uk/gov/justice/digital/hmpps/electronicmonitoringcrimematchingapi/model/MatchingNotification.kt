package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model

data class MatchingNotification(
  val type: String,
  val crimeBatchId: String,
)
