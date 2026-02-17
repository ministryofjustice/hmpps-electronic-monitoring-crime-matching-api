package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.Person

data class PersonResponse(
  val personId: Long,
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
    name = entity.personName,
    nomisId = entity.nomisId,
    pncRef = entity.pncRef,
    dateOfBirth = entity.dob,
    probationPractitioner = entity.probationPractitioner,
    address = "${entity.street} ${entity.city} ${entity.zip}",
    deviceActivations = entity.deviceActivations.map {
      DeviceActivationResponse(it)
    },
  )
}
