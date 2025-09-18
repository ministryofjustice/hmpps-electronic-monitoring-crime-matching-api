package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

data class AthenaPersonDto(
  val personId: Long,
  val personName: String,
  val uIdNomis: String?,
  val uDob: String?,
  val zip: String?,
  val city: String?,
  val street: String?,
  val deviceId: Long?,
  val deviceActivationId: Long?,
  val deviceActivationDate: String?,
  val deviceDeactivationDate: String?,
)
