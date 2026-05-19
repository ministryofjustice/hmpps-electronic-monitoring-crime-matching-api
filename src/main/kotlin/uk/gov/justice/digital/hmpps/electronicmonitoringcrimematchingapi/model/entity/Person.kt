package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity

data class Person(
  val personId: String,
  val firstName: String,
  val lastName: String,
  val nomisId: String,
  val pncRef: String,
  val probationPractitioner: String,
  val dateOfBirth: String,
  val postcode: String,
  val cityOrTown: String,
  val street: String,
  val deviceActivations: MutableList<DeviceActivation>,
)
