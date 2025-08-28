package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena

data class AthenaPersonDTO(
  val personId: String,
  val personName: String,
  val uIdNomis: String?,
  val uDob: String?,
  val zip: String?,
  val city: String?,
  val street: String?,
  val deviceId: String?,
  val deviceActivationId: String?,
  val deviceActivationDate: String?,
  val deviceDeactivationDate: String?,
)
