package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena

import com.fasterxml.jackson.annotation.JsonProperty

data class AthenaSubjectDTO(
  val personId: String,
  @JsonProperty("person_name")
  val name: String?,
  val address: String?,
  val dateOfBirth: String?,
  val deviceId: String?,
  val nomisId: String,
  val orderStartDate: String?,
  val orderEndDate: String?,
  val tagStartDate: String?,
  val tagEndDate: String?,
)
