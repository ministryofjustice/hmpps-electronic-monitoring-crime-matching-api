package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.person

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.entity.person.Person
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.deviceactivation.DeviceActivationDto

data class PersonDto(
  val personId: Int,
  val name: String,
  val nomisId: String?,
  val pncRef: String,
  val dateOfBirth: String?,
  val probationPractitioner: String?,
  val address: String?,
  val deviceActivations: List<DeviceActivationDto>?,
) {
  constructor(entity: Person) : this(
    personId = entity.personId,
    name = entity.personName,
    nomisId = entity.uIdNomis,
    pncRef = "",
    dateOfBirth = entity.uDob,
    probationPractitioner = "",
    address = "${entity.street} ${entity.city} ${entity.zip}",
    deviceActivations = null,
  )
}
