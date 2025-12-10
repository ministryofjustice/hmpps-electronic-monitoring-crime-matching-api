package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

data class MatchingNotification(
  val type: String,

  @JsonProperty("crime_batch_id")
  val crimeBatchId: UUID,
)
