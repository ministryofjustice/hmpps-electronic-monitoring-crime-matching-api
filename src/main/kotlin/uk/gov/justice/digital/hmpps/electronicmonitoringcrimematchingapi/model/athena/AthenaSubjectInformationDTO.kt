package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena

import com.fasterxml.jackson.annotation.JsonProperty

data class AthenaSubjectInformationDTO(
  val nomisId: String,
  @JsonProperty("full_name")
  val name: String?,
  val dateOfBirth: String?,
  val address: String?,
  val orderStartDate: String?,
  val orderEndDate: String?,
  val deviceId: String?,
  val tagPeriodStartDate: String?,
  val tagPeriodEndDate: String?
)