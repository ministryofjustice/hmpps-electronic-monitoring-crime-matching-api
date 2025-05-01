package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model

import com.fasterxml.jackson.annotation.JsonProperty

data class AthenaSubjectInformationDTO(
  val legacySubjectId: String,
  @JsonProperty("full_name")
  val name: String?,
)
