package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.Person

data class PersonResponse(
  val personId: String,
  val name: String,
  val nomisId: String,
  val pncRef: String,
  val dateOfBirth: String,
  val probationPractitioner: String,
  val address: String,
  val deviceActivations: List<DeviceActivationResponse>,
) {
  constructor(entity: Person) : this(
    personId = entity.personId,
    name = entity.firstName + " " + entity.lastName,
    nomisId = entity.nomisId,
    pncRef = entity.pncRef,
    dateOfBirth = entity.dateOfBirth,
    probationPractitioner = entity.probationPractitioner,
    address = "${entity.street}, ${entity.cityOrTown}, ${entity.postcode}",
    deviceActivations = entity.deviceActivations.map {
      DeviceActivationResponse(it)
    },
  )
}
