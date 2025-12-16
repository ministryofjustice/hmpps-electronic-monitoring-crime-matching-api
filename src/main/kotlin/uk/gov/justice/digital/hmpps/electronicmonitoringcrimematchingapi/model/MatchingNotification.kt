package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model

import com.fasterxml.jackson.annotation.JsonProperty

data class MatchingNotification(
  val type: String,

  @JsonProperty("crime_batch_id")
  val crimeBatchId: String,
)
