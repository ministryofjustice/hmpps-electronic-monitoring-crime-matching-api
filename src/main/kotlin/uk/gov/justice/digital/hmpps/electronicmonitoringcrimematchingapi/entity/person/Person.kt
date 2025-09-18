package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.entity.person

data class Person(
  val personId: Int,
  val personName: String,
  val uIdNomis: String?,
  val uDob: String?,
  val zip: String?,
  val city: String?,
  val street: String?,
  val deviceId: Int?,
  val deviceActivationId: Int?,
  val deviceActivationDate: String?,
  val deviceDeactivationDate: String?,
)
