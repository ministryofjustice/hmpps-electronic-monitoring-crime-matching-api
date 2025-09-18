package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

data class PersonDto(
  val personId: Long,
  val name: String,
  val nomisId: String?,
  val pncRef: String,
  val dateOfBirth: String?,
  val probationPractitioner: String?,
  val address: String?,
  val deviceActivations: List<DeviceActivationDto>?,
)
