package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.person

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.entity.person.Person
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.deviceactivation.DeviceActivationDto

data class PersonDto(
  val personId: String,
  val name: String,
  val nomisId: String?,
  val pncRef: String,
  val dateOfBirth: String?,
  val probationPractitioner: String?,
  val address: String?,
  val deviceActivations: List<DeviceActivationDto>?,
) {
  constructor(dto: Person) : this(
    personId = dto.personId,
    name = dto.personName,
    nomisId = dto.uIdNomis,
    pncRef = "",
    dateOfBirth = dto.uDob,
    probationPractitioner = "",
    address = "${dto.street} ${dto.city} ${dto.zip}",
    deviceActivations = null,
  )
}
