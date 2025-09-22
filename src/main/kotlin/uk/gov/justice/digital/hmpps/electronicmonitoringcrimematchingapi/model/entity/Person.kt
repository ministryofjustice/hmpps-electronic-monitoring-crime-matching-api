package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity

data class Person(
  val personId: Long,
  val personName: String,
  val nomisId: String,
  val pncRef: String,
  val probationPractitioner: String,
  val dob: String,
  val zip: String,
  val city: String,
  val street: String,
  val deviceActivations: MutableList<DeviceActivation>,
)
