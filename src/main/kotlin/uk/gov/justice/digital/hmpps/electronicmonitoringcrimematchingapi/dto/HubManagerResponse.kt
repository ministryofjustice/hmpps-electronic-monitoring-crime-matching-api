package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

data class HubManagerResponse(
  val id: String,
  val name: String,
  val occupation: String,
  val hasSignature: Boolean,
)
